package handlers

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os/exec"
	"strconv"
	"strings"
)

// DockerContainer represents a single container's data.
type DockerContainer struct {
	ID             string       `json:"id"`
	ShortID        string       `json:"short_id"`
	Name           string       `json:"name"`
	Image          string       `json:"image"`
	Status         string       `json:"status"`
	State          string       `json:"state"`
	Created        int64        `json:"created"`
	Ports          []PortMapping `json:"ports"`
	CPUPercent     float64      `json:"cpu_percent"`
	MemUsedBytes   int64        `json:"mem_used_bytes"`
	MemLimitBytes  int64        `json:"mem_limit_bytes"`
	NetworkRxBytes int64        `json:"network_rx_bytes"`
	NetworkTxBytes int64        `json:"network_tx_bytes"`
}

// PortMapping maps a host port to a container port.
type PortMapping struct {
	ContainerPort int    `json:"container_port"`
	HostPort      int    `json:"host_port"`
	Protocol      string `json:"protocol"`
}

// DockerImage represents a single Docker image.
type DockerImage struct {
	ID        string   `json:"id"`
	Tags      []string `json:"tags"`
	SizeBytes int64    `json:"size_bytes"`
	Created   int64    `json:"created"`
}

type dockerContainersResponse struct {
	Containers []DockerContainer `json:"containers"`
}

type dockerImagesResponse struct {
	Images []DockerImage `json:"images"`
}

type dockerLogsResponse struct {
	Lines []string `json:"lines"`
}

type dockerActionRequest struct {
	Action string `json:"action"`
}

// dockerAvailable checks whether the docker binary is accessible.
func dockerAvailable() bool {
	_, err := exec.LookPath("docker")
	return err == nil
}

// DockerContainersHandler handles GET /api/v1/docker/containers
func DockerContainersHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	if !dockerAvailable() {
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]string{"error": "docker not available"})
		return
	}

	// List containers with basic info.
	psOut, err := exec.Command("docker", "ps", "-a",
		"--format", "{{.ID}}\t{{.Names}}\t{{.Image}}\t{{.Status}}\t{{.State}}\t{{.Ports}}").Output()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	// Build a map of container stats (cpu / mem) keyed by container ID prefix.
	statsMap := map[string][2]string{} // id -> [cpu%, mem_usage/limit]
	statsOut, err := exec.Command("docker", "stats", "--no-stream",
		"--format", "{{.ID}}\t{{.CPUPerc}}\t{{.MemUsage}}").Output()
	if err == nil {
		for _, line := range strings.Split(strings.TrimSpace(string(statsOut)), "\n") {
			parts := strings.Split(line, "\t")
			if len(parts) == 3 {
				statsMap[parts[0]] = [2]string{parts[1], parts[2]}
			}
		}
	}

	var containers []DockerContainer
	for _, line := range strings.Split(strings.TrimSpace(string(psOut)), "\n") {
		if line == "" {
			continue
		}
		parts := strings.Split(line, "\t")
		if len(parts) < 5 {
			continue
		}
		id := parts[0]
		shortID := id
		if len(id) > 12 {
			shortID = id[:12]
		}
		name := strings.TrimPrefix(parts[1], "/")
		image := parts[2]
		status := parts[3]
		state := parts[4]
		portsRaw := ""
		if len(parts) > 5 {
			portsRaw = parts[5]
		}

		ports := parsePorts(portsRaw)
		cpuPct, memUsed, memLimit := parseStats(statsMap, shortID)

		containers = append(containers, DockerContainer{
			ID:            id,
			ShortID:       shortID,
			Name:          name,
			Image:         image,
			Status:        status,
			State:         state,
			Ports:         ports,
			CPUPercent:    cpuPct,
			MemUsedBytes:  memUsed,
			MemLimitBytes: memLimit,
		})
	}

	if containers == nil {
		containers = []DockerContainer{}
	}
	json.NewEncoder(w).Encode(dockerContainersResponse{Containers: containers})
}

// DockerImagesHandler handles GET /api/v1/docker/images
func DockerImagesHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	if !dockerAvailable() {
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]string{"error": "docker not available"})
		return
	}

	out, err := exec.Command("docker", "images",
		"--format", "{{.ID}}\t{{.Repository}}:{{.Tag}}\t{{.Size}}\t{{.CreatedAt}}").Output()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": err.Error()})
		return
	}

	var images []DockerImage
	for _, line := range strings.Split(strings.TrimSpace(string(out)), "\n") {
		if line == "" {
			continue
		}
		parts := strings.SplitN(line, "\t", 4)
		if len(parts) < 2 {
			continue
		}
		id := parts[0]
		tag := parts[1]
		sizeBytes := int64(0)
		if len(parts) >= 3 {
			sizeBytes = parseDockerSize(parts[2])
		}
		images = append(images, DockerImage{
			ID:        id,
			Tags:      []string{tag},
			SizeBytes: sizeBytes,
		})
	}

	if images == nil {
		images = []DockerImage{}
	}
	json.NewEncoder(w).Encode(dockerImagesResponse{Images: images})
}

// DockerContainerActionHandler handles POST /api/v1/docker/containers/{id}/action
func DockerContainerActionHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	if !dockerAvailable() {
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]string{"error": "docker not available"})
		return
	}

	// Extract container ID from path: /api/v1/docker/containers/{id}/action
	id := extractDockerContainerID(r.URL.Path, "/action")
	if id == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "container ID required"})
		return
	}

	var req dockerActionRequest
	if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid JSON: " + err.Error()})
		return
	}

	allowed := map[string]bool{"start": true, "stop": true, "restart": true, "remove": true, "rm": true, "pause": true, "unpause": true}
	action := strings.ToLower(req.Action)
	if !allowed[action] {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "invalid action: " + action})
		return
	}
	// Normalize remove -> rm
	if action == "remove" {
		action = "rm"
	}

	out, err := exec.Command("docker", action, id).CombinedOutput()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]interface{}{
			"success": false,
			"message": strings.TrimSpace(string(out)),
		})
		return
	}

	json.NewEncoder(w).Encode(map[string]interface{}{
		"success": true,
		"message": fmt.Sprintf("Container %s: %s succeeded", id, action),
	})
}

// DockerContainerLogsHandler handles GET /api/v1/docker/containers/{id}/logs
func DockerContainerLogsHandler(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, `{"error":"method not allowed"}`, http.StatusMethodNotAllowed)
		return
	}
	w.Header().Set("Content-Type", "application/json")

	if !dockerAvailable() {
		w.WriteHeader(http.StatusServiceUnavailable)
		json.NewEncoder(w).Encode(map[string]string{"error": "docker not available"})
		return
	}

	// Extract container ID from path: /api/v1/docker/containers/{id}/logs
	id := extractDockerContainerID(r.URL.Path, "/logs")
	if id == "" {
		w.WriteHeader(http.StatusBadRequest)
		json.NewEncoder(w).Encode(map[string]string{"error": "container ID required"})
		return
	}

	linesStr := r.URL.Query().Get("lines")
	lines := 100
	if linesStr != "" {
		if n, err := strconv.Atoi(linesStr); err == nil && n > 0 {
			lines = n
		}
	}

	out, err := exec.Command("docker", "logs", "--tail", strconv.Itoa(lines), id).CombinedOutput()
	if err != nil {
		w.WriteHeader(http.StatusInternalServerError)
		json.NewEncoder(w).Encode(map[string]string{"error": string(out)})
		return
	}

	logLines := strings.Split(strings.TrimRight(string(out), "\n"), "\n")
	if len(logLines) == 1 && logLines[0] == "" {
		logLines = []string{}
	}
	json.NewEncoder(w).Encode(dockerLogsResponse{Lines: logLines})
}

// extractDockerContainerID extracts the container ID segment from a path like:
// /api/v1/docker/containers/{id}/action  (suffix = "/action")
func extractDockerContainerID(path, suffix string) string {
	trimmed := strings.TrimPrefix(path, "/api/v1/docker/containers/")
	trimmed = strings.TrimSuffix(trimmed, suffix)
	trimmed = strings.Trim(trimmed, "/")
	return trimmed
}

// parsePorts parses Docker port mapping strings like "0.0.0.0:8080->80/tcp, :::8080->80/tcp"
func parsePorts(raw string) []PortMapping {
	if raw == "" {
		return []PortMapping{}
	}
	var result []PortMapping
	seen := map[string]bool{}
	entries := strings.Split(raw, ", ")
	for _, entry := range entries {
		entry = strings.TrimSpace(entry)
		// Format: 0.0.0.0:8080->80/tcp
		arrowIdx := strings.Index(entry, "->")
		if arrowIdx < 0 {
			continue
		}
		left := entry[:arrowIdx]   // 0.0.0.0:8080
		right := entry[arrowIdx+2:] // 80/tcp

		proto := "tcp"
		if slashIdx := strings.LastIndex(right, "/"); slashIdx >= 0 {
			proto = right[slashIdx+1:]
			right = right[:slashIdx]
		}

		containerPort, err := strconv.Atoi(right)
		if err != nil {
			continue
		}

		hostPort := 0
		if colonIdx := strings.LastIndex(left, ":"); colonIdx >= 0 {
			hostPort, _ = strconv.Atoi(left[colonIdx+1:])
		}

		key := fmt.Sprintf("%d->%d/%s", hostPort, containerPort, proto)
		if seen[key] {
			continue
		}
		seen[key] = true
		result = append(result, PortMapping{
			ContainerPort: containerPort,
			HostPort:      hostPort,
			Protocol:      proto,
		})
	}
	return result
}

// parseStats looks up CPU % and memory stats for the given container short ID.
func parseStats(statsMap map[string][2]string, shortID string) (cpuPct float64, memUsed, memLimit int64) {
	data, ok := statsMap[shortID]
	if !ok {
		return 0, 0, 0
	}

	// CPU: "0.25%"
	cpuStr := strings.TrimSuffix(data[0], "%")
	cpuPct, _ = strconv.ParseFloat(cpuStr, 64)

	// Mem: "128MiB / 2GiB"
	memParts := strings.Split(data[1], " / ")
	if len(memParts) == 2 {
		memUsed = parseMemValue(memParts[0])
		memLimit = parseMemValue(memParts[1])
	}
	return
}

// parseMemValue converts strings like "128MiB", "1.5GiB", "512kB" to bytes.
func parseMemValue(s string) int64 {
	s = strings.TrimSpace(s)
	multipliers := []struct {
		suffix string
		factor int64
	}{
		{"GiB", 1 << 30},
		{"MiB", 1 << 20},
		{"KiB", 1 << 10},
		{"GB", 1_000_000_000},
		{"MB", 1_000_000},
		{"kB", 1_000},
		{"B", 1},
	}
	for _, m := range multipliers {
		if strings.HasSuffix(s, m.suffix) {
			numStr := strings.TrimSuffix(s, m.suffix)
			if f, err := strconv.ParseFloat(strings.TrimSpace(numStr), 64); err == nil {
				return int64(f * float64(m.factor))
			}
		}
	}
	return 0
}

// parseDockerSize converts strings like "128MB", "1.23GB" to bytes.
func parseDockerSize(s string) int64 {
	return parseMemValue(s)
}
