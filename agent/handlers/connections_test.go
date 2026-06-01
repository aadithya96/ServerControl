package handlers

import "testing"

func TestHexToIP4(t *testing.T) {
	cases := []struct {
		hex  string
		want string
	}{
		// /proc stores addresses little-endian: 0100007F -> 127.0.0.1
		{"0100007F", "127.0.0.1"},
		{"00000000", "0.0.0.0"},
		{"0101A8C0", "192.168.1.1"},
		{"FFFFFFFF", "255.255.255.255"},
		// Invalid hex is returned unchanged.
		{"zzzz", "zzzz"},
		// Too short is returned unchanged.
		{"0100", "0100"},
	}
	for _, c := range cases {
		if got := hexToIP4(c.hex); got != c.want {
			t.Errorf("hexToIP4(%q) = %q, want %q", c.hex, got, c.want)
		}
	}
}

func TestHexToIP6(t *testing.T) {
	cases := []struct {
		hex  string
		want string
	}{
		// ::1 as stored in /proc/net/tcp6
		{"00000000000000000000000001000000", "0:0:0:0:0:0:0:1"},
		// all zeros -> unspecified address
		{"00000000000000000000000000000000", "0:0:0:0:0:0:0:0"},
		// Invalid hex returned unchanged.
		{"nothex", "nothex"},
		// Too short returned unchanged.
		{"0100007F", "0100007F"},
	}
	for _, c := range cases {
		if got := hexToIP6(c.hex); got != c.want {
			t.Errorf("hexToIP6(%q) = %q, want %q", c.hex, got, c.want)
		}
	}
}

func TestHexToPort(t *testing.T) {
	cases := []struct {
		hex  string
		want int
	}{
		{"0050", 80},
		{"1F90", 8080},
		{"0016", 22},
		{"0000", 0},
		{"FFFF", 65535},
	}
	for _, c := range cases {
		if got := hexToPort(c.hex); got != c.want {
			t.Errorf("hexToPort(%q) = %d, want %d", c.hex, got, c.want)
		}
	}
}
