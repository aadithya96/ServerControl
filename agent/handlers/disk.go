package handlers

import (
	"bufio"
	"encoding/json"
	"net/http"
	"os"
	"strconv"
	"strings"
	"syscall"
	"time"
)

type MountInfo struct {
	Device          string  `json:"device"`
	MountPoint      string  `json:"mount_point"`
	FSType          string  `json:"fs_type"`
	TotalBytes      uint64  `json:"total_bytes"`
	UsedBytes       uint64  `json:"used_bytes"`
	FreeBytes       uint64  `json:"free_bytes"`
	UsagePercent    float64 `json:"usage_percent"`
	ReadBytesPerSec int64   `json:"read_bytes_per_sec"`
	WriteBytesPerSec int64  `json:"write_bytes_per_sec"`
	IOWaitPercent   float64 `json:"io_wait_percent"`
}

type DiskResponse struct {
	Mounts []MountInfo `json:"mounts"`
}

type diskIOStat struct {
	readSectors  uint64
	writeSectors uint64
	readTicks    uint64
	writeTicks   uint64
}

func readDiskStats() map[string]diskIOStat {
	result := make(map[string]diskIOStat)
	f, err := os.Open("/proc/diskstats")
	if err != nil {
		return result
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 14 {
			continue
		}
		dev := fields[2]
		readSectors, _ := strconv.ParseUint(fields[5], 10, 64)
		readTicks, _ := strconv.ParseUint(fields[6], 10, 64)
		writeSectors, _ := strconv.ParseUint(fields[9], 10, 64)
		writeTicks, _ := strconv.ParseUint(fields[10], 10, 64)
		result[dev] = diskIOStat{
			readSectors:  readSectors,
			writeSectors: writeSectors,
			readTicks:    readTicks,
			writeTicks:   writeTicks,
		}
	}
	return result
}

func getMounts() []MountInfo {
	f, err := os.Open("/proc/mounts")
	if err != nil {
		return nil
	}
	defer f.Close()

	// Sample IO before
	stats1 := readDiskStats()
	time.Sleep(500 * time.Millisecond)
	stats2 := readDiskStats()

	var mounts []MountInfo
	seen := make(map[string]bool)
	scanner := bufio.NewScanner(f)
	for scanner.Scan() {
		fields := strings.Fields(scanner.Text())
		if len(fields) < 3 {
			continue
		}
		device := fields[0]
		mountPoint := fields[1]
		fsType := fields[2]

		// Skip pseudo filesystems
		if !strings.HasPrefix(device, "/") {
			continue
		}
		if seen[mountPoint] {
			continue
		}
		seen[mountPoint] = true

		var stat syscall.Statfs_t
		if err := syscall.Statfs(mountPoint, &stat); err != nil {
			continue
		}
		total := stat.Blocks * uint64(stat.Bsize)
		free := stat.Bavail * uint64(stat.Bsize)
		used := total - stat.Bfree*uint64(stat.Bsize)
		var usagePct float64
		if total > 0 {
			usagePct = float64(used) / float64(total) * 100.0
		}

		// Get device name (strip /dev/ prefix for diskstats lookup)
		devName := device
		if idx := strings.LastIndex(device, "/"); idx >= 0 {
			devName = device[idx+1:]
		}
		var readBPS, writeBPS int64
		var ioWait float64
		if s1, ok1 := stats1[devName]; ok1 {
			if s2, ok2 := stats2[devName]; ok2 {
				sectorSize := int64(512)
				readBPS = int64(s2.readSectors-s1.readSectors) * sectorSize * 2 // per 500ms → per sec
				writeBPS = int64(s2.writeSectors-s1.writeSectors) * sectorSize * 2
				totalTicks := (s2.readTicks - s1.readTicks) + (s2.writeTicks - s1.writeTicks)
				ioWait = float64(totalTicks) / 500.0 * 100.0 // ms / 500ms window
				if ioWait > 100.0 {
					ioWait = 100.0
				}
			}
		}

		mounts = append(mounts, MountInfo{
			Device:          device,
			MountPoint:      mountPoint,
			FSType:          fsType,
			TotalBytes:      total,
			UsedBytes:       used,
			FreeBytes:       free,
			UsagePercent:    usagePct,
			ReadBytesPerSec: readBPS,
			WriteBytesPerSec: writeBPS,
			IOWaitPercent:   ioWait,
		})
	}
	return mounts
}

func DiskHandler(w http.ResponseWriter, r *http.Request) {
	mounts := getMounts()
	if mounts == nil {
		mounts = []MountInfo{}
	}
	w.Header().Set("Content-Type", "application/json")
	json.NewEncoder(w).Encode(DiskResponse{Mounts: mounts})
}
