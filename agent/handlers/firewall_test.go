package handlers

import "testing"

func TestParseIPTables(t *testing.T) {
	const sample = `Chain INPUT (policy DROP)
num   pkts bytes target     prot opt source               destination
1      100  6400 ACCEPT     all  --  0.0.0.0/0            0.0.0.0/0
2       50  3000 ACCEPT     tcp  --  0.0.0.0/0            0.0.0.0/0            tcp dpt:22

Chain FORWARD (policy ACCEPT)
num   pkts bytes target     prot opt source               destination

Chain OUTPUT (policy ACCEPT)
num   pkts bytes target     prot opt source               destination
1        0     0 ACCEPT     all  --  0.0.0.0/0            0.0.0.0/0
`

	chains := parseIPTables(sample)
	if len(chains) != 3 {
		t.Fatalf("expected 3 chains, got %d: %+v", len(chains), chains)
	}

	input := chains[0]
	if input.Name != "INPUT" {
		t.Errorf("chain[0].Name = %q, want INPUT", input.Name)
	}
	if input.Policy != "DROP" {
		t.Errorf("chain[0].Policy = %q, want DROP", input.Policy)
	}
	if len(input.Rules) != 2 {
		t.Fatalf("INPUT: expected 2 rules, got %d: %+v", len(input.Rules), input.Rules)
	}

	r0 := input.Rules[0]
	if r0.ID != "INPUT-1" || r0.Num != 1 || r0.Target != "ACCEPT" || r0.Protocol != "all" {
		t.Errorf("INPUT rule[0] unexpected: %+v", r0)
	}
	if r0.Packets != 100 || r0.Bytes != 6400 {
		t.Errorf("INPUT rule[0] counters: pkts=%d bytes=%d, want 100/6400", r0.Packets, r0.Bytes)
	}
	if r0.Source != "0.0.0.0/0" || r0.Destination != "0.0.0.0/0" {
		t.Errorf("INPUT rule[0] addrs: src=%q dst=%q", r0.Source, r0.Destination)
	}

	r1 := input.Rules[1]
	if r1.ID != "INPUT-2" || r1.Num != 2 || r1.Protocol != "tcp" {
		t.Errorf("INPUT rule[1] unexpected: %+v", r1)
	}
	if r1.Options != "tcp dpt:22" {
		t.Errorf("INPUT rule[1].Options = %q, want %q", r1.Options, "tcp dpt:22")
	}

	if chains[1].Name != "FORWARD" || len(chains[1].Rules) != 0 {
		t.Errorf("FORWARD chain unexpected: %+v", chains[1])
	}
	if chains[2].Name != "OUTPUT" || len(chains[2].Rules) != 1 {
		t.Errorf("OUTPUT chain unexpected: %+v", chains[2])
	}
}

func TestParseIPTablesEmpty(t *testing.T) {
	if chains := parseIPTables(""); len(chains) != 0 {
		t.Errorf("expected no chains for empty input, got %+v", chains)
	}
}
