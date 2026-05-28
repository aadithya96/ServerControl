package handlers

import (
	"bufio"
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"strconv"
	"strings"
	"time"
)

type StatsResponse struct {
	Hostname       string  `json:"hostname"`
	UptimeSeconds  uint64  `json:"uptime_seconds"`
	LoadAvg1       float64 `json:"load_avg_1"`
	LoadAvg5       float64 `json:"load_avg_5"`
	LoadAvg15      float64 `json:"load_avg_15"`
	CPUPercent     float64 `json:"cpu_percent"`
	CPUCores       int     `json:"cpu_cores"`
	MemTotalBytes  uint64  `json:"mem_total_bytes"`
	MemUsedBytes   uint64  `json:"mem_used_bytes"`
	MemFreeBytes   uint64  `json:"mem_free_bytes"`
	SwapTotalBytes uint64  `json:"swap_total_bytes"`
	SwapUsedBytes  uint64  `json:"swap_used_bytes"`
}

type cpuStat struct {
	user   uint64
	nice   uint64
	system uint64
	idle   uint64
	iowait uint64
	irq    uint64
	softirq uint64
}

func readCPUStat() (cpuStat, error) {
	f, err := os.Open("/proc/stat")
	if err != nil {
		return cpuStat{}, err
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, "cpu ") {
			fields := strings.Fields(line)
			if len(fields) < 8 {
				break
			}
			parse := func(s string) uint64 {
				v, _ := strconv.ParseUint(s, 10, 64)
				return v
			}
			return cpuStat{
				user:    parse(fields[1]),
				nice:    parse(fields[2]),
				system:  parse(fields[3]),
				idle:    parse(fields[4]),
				iowait:  parse(fields[5]),
				irq:     parse(fields[6]),
				softirq: parse(fields[7]),
			}, nil
		}
	}
	return cpuStat{}, fmt.Errorf("cpu line not found in /proc/stat")
}

func getCPUPercent() (float64, error) {
	s1, err := readCPUStat()
	if err != nil {
		return 0, err
	}
	time.Sleep(500 * time.Millisecond)
	s2, err := readCPUStat()
	if err != nil {
		return 0, err
	}

	idle1 := s1.idle + s1.iowait
	total1 := s1.user + s1.nice + s1.system + idle1 + s1.irq + s1.softirq
	idle2 := s2.idle + s2.iowait
	total2 := s2.user + s2.nice + s2.system + idle2 + s2.irq + s2.softirq

	deltaTotal := total2 - total1
	deltaIdle := idle2 - idle1

	if deltaTotal == 0 {
		return 0, nil
	}
	return float64(deltaTotal-deltaIdle) / float64(deltaTotal) * 100.0, nil
}

func countCPUCores() int {
	f, err := os.Open("/proc/stat")
	if err != nil {
		return 1
	}
	defer f.Close()
	count := 0
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := scanner.Text()
		if strings.HasPrefix(line, "cpu") && len(line) > 3 && line[3] != ' ' {
			count++
		}
	}
	if count == 0 {
		return 1
	}
	return count
}

func readMemInfo() (total, used, free, swapTotal, swapUsed uint64, err error) {
	f, err := os.Open("/proc/meminfo")
	if err != nil {
		return
	}
	defer f.Close()

	m := make(map[string]uint64)
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		line := scanner.Text()
		parts := strings.Fields(line)
		if len(parts) >= 2 {
			key := strings.TrimSuffix(parts[0], ":")
			val, _ := strconv.ParseUint(parts[1], 10, 64)
			m[key] = val * 1024 // convert kB to bytes
		}
	}

	total = m["MemTotal"]
	free = m["MemAvailable"]
	used = total - free
	swapTotal = m["SwapTotal"]
	swapFree := m["SwapFree"]
	swapUsed = swapTotal - swapFree
	return
}

func readUptime() (uint64, error) {
	f, err := os.Open("/proc/uptime")
	if err != nil {
		return 0, err
	}
	defer f.Close()
	var uptimeF float64
	fmt.Fscanf(f, "%f", &uptimeF)
	return uint64(uptimeF), nil
}

func readLoadAvg() (float64, float64, float64, error) {
	f, err := os.Open("/proc/loadavg")
	if err != nil {
		return 0, 0, 0, err
	}
	defer f.Close()
	var l1, l5, l15 float64
	fmt.Fscanf(f, "%f %f %f", &l1, &l5, &l15)
	return l1, l5, l15, nil
}

func StatsHandler(w http.ResponseWriter, r *http.Request) {
	hostname, _ := os.Hostname()
	uptime, _ := readUptime()
	l1, l5, l15, _ := readLoadAvg()
	cpuPct, _ := getCPUPercent()
	cores := countCPUCores()
	memTotal, memUsed, memFree, swapTotal, swapUsed, _ := readMemInfo()

	resp := StatsResponse{
		Hostname:       hostname,
		UptimeSeconds:  uptime,
		LoadAvg1:       l1,
		LoadAvg5:       l5,
		LoadAvg15:      l15,
		CPUPercent:     cpuPct,
		CPUCores:       cores,
		MemTotalBytes:  memTotal,
		MemUsedBytes:   memUsed,
		MemFreeBytes:   memFree,
		SwapTotalBytes: swapTotal,
		SwapUsedBytes:  swapUsed,
	}

	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(resp)
}
