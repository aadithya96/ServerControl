package handlers

import (
	"bufio"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

type ConnectionItem struct {
	Protocol      string `json:"protocol"`
	LocalAddress  string `json:"local_address"`
	LocalPort     int    `json:"local_port"`
	RemoteAddress string `json:"remote_address"`
	RemotePort    int    `json:"remote_port"`
	State         string `json:"state"`
	PID           int    `json:"pid"`
	ProcessName   string `json:"process_name"`
}

type ConnectionResponse struct {
	Connections []ConnectionItem `json:"connections"`
}

var tcpStates = map[string]string{
	"01": "ESTABLISHED",
	"02": "SYN_SENT",
	"03": "SYN_RECV",
	"04": "FIN_WAIT1",
	"05": "FIN_WAIT2",
	"06": "TIME_WAIT",
	"07": "CLOSE",
	"08": "CLOSE_WAIT",
	"09": "LAST_ACK",
	"0A": "LISTEN",
	"0B": "CLOSING",
}

func hexToIP4(hexIP string) string {
	b, err := hex.DecodeString(hexIP)
	if err != nil || len(b) < 4 {
		return hexIP
	}
	// Little-endian
	return fmt.Sprintf("%d.%d.%d.%d", b[3], b[2], b[1], b[0])
}

func hexToIP6(hexIP string) string {
	b, err := hex.DecodeString(hexIP)
	if err != nil || len(b) < 16 {
		return hexIP
	}
	// IPv6 is stored as 4 little-endian 32-bit words
	var groups [8]uint16
	for i := 0; i < 4; i++ {
		word := uint32(b[i*4+3])<<24 | uint32(b[i*4+2])<<16 | uint32(b[i*4+1])<<8 | uint32(b[i*4])
		groups[i*2] = uint16(word >> 16)
		groups[i*2+1] = uint16(word & 0xffff)
	}
	return fmt.Sprintf("%x:%x:%x:%x:%x:%x:%x:%x",
		groups[0], groups[1], groups[2], groups[3],
		groups[4], groups[5], groups[6], groups[7])
}

func hexToPort(hexPort string) int {
	v, _ := strconv.ParseInt(hexPort, 16, 32)
	return int(v)
}

// buildInodeMap builds a map from inode number to PID
func buildInodeMap() map[string]int {
	result := make(map[string]int)
	pidDirs, _ := filepath.Glob("/proc/[0-9]*/fd/*")
	for _, fdPath := range pidDirs {
		target, err := os.Readlink(fdPath)
		if err != nil {
			continue
		}
		// socket:[12345]
		if strings.HasPrefix(target, "socket:[") {
			inode := strings.TrimSuffix(strings.TrimPrefix(target, "socket:["), "]")
			// Extract PID from path
			parts := strings.Split(fdPath, "/")
			if len(parts) >= 3 {
				pid, err := strconv.Atoi(parts[2])
				if err == nil {
					result[inode] = pid
				}
			}
		}
	}
	return result
}

func readNetFile(path, proto string, isIPv6 bool, inodeMap map[string]int, pidNameMap map[int]string) []ConnectionItem {
	f, err := os.Open(path)
	if err != nil {
		return nil
	}
	defer f.Close()

	var conns []ConnectionItem
	scanner := bufio.NewScanner(f)
	first := true
	for scanner.Scan() {
		if first {
			first = false
			continue // skip header
		}
		fields := strings.Fields(scanner.Text())
		if len(fields) < 10 {
			continue
		}

		localParts := strings.Split(fields[1], ":")
		remoteParts := strings.Split(fields[2], ":")
		stateHex := strings.ToUpper(fields[3])
		inode := fields[9]

		if len(localParts) != 2 || len(remoteParts) != 2 {
			continue
		}

		var localAddr, remoteAddr string
		if isIPv6 {
			localAddr = hexToIP6(localParts[0])
			remoteAddr = hexToIP6(remoteParts[0])
		} else {
			localAddr = hexToIP4(localParts[0])
			remoteAddr = hexToIP4(remoteParts[0])
		}

		localPort := hexToPort(localParts[1])
		remotePort := hexToPort(remoteParts[1])

		state := tcpStates[stateHex]
		if state == "" {
			state = stateHex
		}
		// For UDP, state is not meaningful
		if proto == "udp" {
			state = "UNCONN"
		}

		pid := inodeMap[inode]
		processName := pidNameMap[pid]

		conns = append(conns, ConnectionItem{
			Protocol:      proto,
			LocalAddress:  localAddr,
			LocalPort:     localPort,
			RemoteAddress: remoteAddr,
			RemotePort:    remotePort,
			State:         state,
			PID:           pid,
			ProcessName:   processName,
		})
	}
	return conns
}

func buildPIDNameMap() map[int]string {
	result := make(map[int]string)
	dirs, _ := filepath.Glob("/proc/[0-9]*")
	for _, dir := range dirs {
		base := filepath.Base(dir)
		pid, err := strconv.Atoi(base)
		if err != nil {
			continue
		}
		commData, err := os.ReadFile(dir + "/comm")
		if err != nil {
			continue
		}
		result[pid] = strings.TrimSpace(string(commData))
	}
	return result
}

func ConnectionsHandler(w http.ResponseWriter, r *http.Request) {
	proto := strings.ToLower(r.URL.Query().Get("proto"))
	if proto == "" {
		proto = "all"
	}

	inodeMap := buildInodeMap()
	pidNameMap := buildPIDNameMap()

	var conns []ConnectionItem
	if proto == "tcp" || proto == "all" {
		conns = append(conns, readNetFile("/proc/net/tcp", "tcp", false, inodeMap, pidNameMap)...)
		conns = append(conns, readNetFile("/proc/net/tcp6", "tcp", true, inodeMap, pidNameMap)...)
	}
	if proto == "udp" || proto == "all" {
		conns = append(conns, readNetFile("/proc/net/udp", "udp", false, inodeMap, pidNameMap)...)
		conns = append(conns, readNetFile("/proc/net/udp6", "udp", true, inodeMap, pidNameMap)...)
	}

	if conns == nil {
		conns = []ConnectionItem{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(ConnectionResponse{Connections: conns})
}
