package handlers

import (
	"strings"
	"testing"
)

func TestParseNetDev(t *testing.T) {
	const sample = `Inter-|   Receive                                                |  Transmit
 face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed
    lo: 1000      10    0    0    0     0          0         0     1000      10    0    0    0     0       0          0
  eth0: 5000      50    0    0    0     0          0         0     2000      20    0    0    0     0       0          0
 wlan0:    0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0
`

	counters, err := parseNetDev(strings.NewReader(sample))
	if err != nil {
		t.Fatalf("parseNetDev returned error: %v", err)
	}

	if len(counters) != 3 {
		t.Fatalf("expected 3 interfaces, got %d: %+v", len(counters), counters)
	}

	cases := []struct {
		name   string
		rx, tx uint64
	}{
		{"lo", 1000, 1000},
		{"eth0", 5000, 2000},
		{"wlan0", 0, 0},
	}
	for _, c := range cases {
		got, ok := counters[c.name]
		if !ok {
			t.Errorf("interface %q missing from result", c.name)
			continue
		}
		if got.rxBytes != c.rx || got.txBytes != c.tx {
			t.Errorf("%s: got rx=%d tx=%d, want rx=%d tx=%d",
				c.name, got.rxBytes, got.txBytes, c.rx, c.tx)
		}
	}
}

func TestParseNetDevSkipsMalformedLines(t *testing.T) {
	// A line with too few fields must be skipped rather than panicking.
	const sample = `Inter-|   Receive | Transmit
 face |bytes ...
  eth0: 5000 50 0 0 0 0 0 0 2000 20 0 0 0 0 0 0
  bad0: 1 2 3
`
	counters, err := parseNetDev(strings.NewReader(sample))
	if err != nil {
		t.Fatalf("parseNetDev returned error: %v", err)
	}
	if _, ok := counters["bad0"]; ok {
		t.Errorf("malformed interface bad0 should have been skipped")
	}
	if got, ok := counters["eth0"]; !ok || got.rxBytes != 5000 || got.txBytes != 2000 {
		t.Errorf("eth0 not parsed correctly: %+v (ok=%v)", got, ok)
	}
}

func TestParseNetDevEmpty(t *testing.T) {
	counters, err := parseNetDev(strings.NewReader(""))
	if err != nil {
		t.Fatalf("parseNetDev returned error: %v", err)
	}
	if len(counters) != 0 {
		t.Errorf("expected empty map, got %+v", counters)
	}
}
