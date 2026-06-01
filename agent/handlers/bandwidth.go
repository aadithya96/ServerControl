package handlers

import (
	"bufio"
	"encoding/json"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

type BandwidthInterface struct {
	Name    string `json:"name"`
	RxBps   uint64 `json:"rx_bps"`
	TxBps   uint64 `json:"tx_bps"`
	RxTotal uint64 `json:"rx_total"`
	TxTotal uint64 `json:"tx_total"`
}

type BandwidthResponse struct {
	Interfaces []BandwidthInterface `json:"interfaces"`
}

type ifaceCounters struct {
	rxBytes uint64
	txBytes uint64
}

// readNetDev parses /proc/net/dev and returns per-interface cumulative byte counters.
func readNetDev() (map[string]ifaceCounters, error) {
	f, err := os.Open("/proc/net/dev")
	if err != nil {
		return nil, err
	}
	defer f.Close()

	result := make(map[string]ifaceCounters)
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := scanner.Text()
		idx := strings.IndexByte(line, ':')
		if idx < 0 {
			// header lines have no ':' in the interface position
			continue
		}
		name := strings.TrimSpace(line[:idx])
		fields := strings.Fields(line[idx+1:])
		// Receive: bytes packets errs drop fifo frame compressed multicast (8)
		// Transmit: bytes packets errs drop fifo colls carrier compressed (8)
		if len(fields) < 16 {
			continue
		}
		rx, _ := strconv.ParseUint(fields[0], 10, 64)
		tx, _ := strconv.ParseUint(fields[8], 10, 64)
		result[name] = ifaceCounters{rxBytes: rx, txBytes: tx}
	}
	return result, nil
}

func BandwidthHandler(w http.ResponseWriter, r *http.Request) {
	w.Header().Set("Content-Type", "application/json")

	first, err := readNetDev()
	if err != nil {
		json.NewEncoder(w).Encode(BandwidthResponse{Interfaces: []BandwidthInterface{}})
		return
	}

	const sampleMs = 500
	time.Sleep(sampleMs * time.Millisecond)

	second, err := readNetDev()
	if err != nil {
		json.NewEncoder(w).Encode(BandwidthResponse{Interfaces: []BandwidthInterface{}})
		return
	}

	interfaces := make([]BandwidthInterface, 0, len(second))
	for name, cur := range second {
		// Skip the loopback interface — it isn't real network traffic.
		if name == "lo" {
			continue
		}
		prev, ok := first[name]
		var rxBps, txBps uint64
		if ok {
			if cur.rxBytes >= prev.rxBytes {
				rxBps = (cur.rxBytes - prev.rxBytes) * 1000 / sampleMs
			}
			if cur.txBytes >= prev.txBytes {
				txBps = (cur.txBytes - prev.txBytes) * 1000 / sampleMs
			}
		}
		interfaces = append(interfaces, BandwidthInterface{
			Name:    name,
			RxBps:   rxBps,
			TxBps:   txBps,
			RxTotal: cur.rxBytes,
			TxTotal: cur.txBytes,
		})
	}

	json.NewEncoder(w).Encode(BandwidthResponse{Interfaces: interfaces})
}
