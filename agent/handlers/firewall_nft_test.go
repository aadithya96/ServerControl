package handlers

import "testing"

const sampleNftJSON = `{
  "nftables": [
    {"metainfo": {"version": "1.0.6", "release_name": "Lester Gooch"}},
    {"table": {"family": "inet", "name": "filter", "handle": 1}},
    {"chain": {"family": "inet", "table": "filter", "name": "input", "handle": 1, "type": "filter", "hook": "input", "prio": 0, "policy": "drop"}},
    {"rule": {"family": "inet", "table": "filter", "chain": "input", "handle": 4, "expr": [
      {"match": {"op": "==", "left": {"payload": {"protocol": "tcp", "field": "dport"}}, "right": 22}},
      {"counter": {"packets": 10, "bytes": 600}},
      {"accept": null}
    ]}},
    {"rule": {"family": "inet", "table": "filter", "chain": "input", "handle": 5, "expr": [
      {"match": {"op": "==", "left": {"payload": {"protocol": "ip", "field": "saddr"}}, "right": {"prefix": {"addr": "10.0.0.0", "len": 8}}}},
      {"drop": null}
    ]}},
    {"chain": {"family": "inet", "table": "filter", "name": "forward", "handle": 2, "policy": "accept"}}
  ]
}`

func TestParseNftables(t *testing.T) {
	chains := parseNftables(sampleNftJSON)
	if len(chains) != 2 {
		t.Fatalf("expected 2 chains, got %d: %+v", len(chains), chains)
	}

	input := chains[0]
	if input.Name != "input" || input.Policy != "drop" {
		t.Errorf("chain[0] = name %q policy %q, want input/drop", input.Name, input.Policy)
	}
	if len(input.Rules) != 2 {
		t.Fatalf("input: expected 2 rules, got %d", len(input.Rules))
	}

	r0 := input.Rules[0]
	if r0.ID != "nft:inet:filter:input:4" {
		t.Errorf("rule[0].ID = %q, want nft:inet:filter:input:4", r0.ID)
	}
	if r0.Num != 4 {
		t.Errorf("rule[0].Num (handle) = %d, want 4", r0.Num)
	}
	if r0.Target != "ACCEPT" {
		t.Errorf("rule[0].Target = %q, want ACCEPT", r0.Target)
	}
	if r0.Protocol != "tcp" {
		t.Errorf("rule[0].Protocol = %q, want tcp", r0.Protocol)
	}
	if r0.Options != "dport 22" {
		t.Errorf("rule[0].Options = %q, want \"dport 22\"", r0.Options)
	}
	if r0.Packets != 10 || r0.Bytes != 600 {
		t.Errorf("rule[0] counters = %d/%d, want 10/600", r0.Packets, r0.Bytes)
	}

	r1 := input.Rules[1]
	if r1.Target != "DROP" {
		t.Errorf("rule[1].Target = %q, want DROP", r1.Target)
	}
	if r1.Source != "10.0.0.0/8" {
		t.Errorf("rule[1].Source = %q, want 10.0.0.0/8", r1.Source)
	}

	if chains[1].Name != "forward" || len(chains[1].Rules) != 0 {
		t.Errorf("chain[1] unexpected: %+v", chains[1])
	}
}

func TestParseNftablesEmptyRuleset(t *testing.T) {
	empty := `{"nftables": [{"metainfo": {"version": "1.0.6"}}]}`
	if chains := parseNftables(empty); len(chains) != 0 {
		t.Errorf("expected no chains for empty ruleset, got %+v", chains)
	}
}

func TestParseNftablesInvalidJSON(t *testing.T) {
	if chains := parseNftables("not json"); chains != nil {
		t.Errorf("expected nil for invalid JSON, got %+v", chains)
	}
}

func TestNftDeleteArgs(t *testing.T) {
	args, err := nftDeleteArgs("nft:inet:filter:input:4")
	if err != nil {
		t.Fatalf("unexpected error: %v", err)
	}
	want := []string{"delete", "rule", "inet", "filter", "input", "handle", "4"}
	if len(args) != len(want) {
		t.Fatalf("args = %v, want %v", args, want)
	}
	for i := range want {
		if args[i] != want[i] {
			t.Errorf("args[%d] = %q, want %q", i, args[i], want[i])
		}
	}
}

func TestNftDeleteArgsInvalid(t *testing.T) {
	bad := []string{
		"INPUT-4",                  // iptables-style
		"nft:inet:filter:input",    // missing handle
		"nft:inet:filter:input:xx", // non-numeric handle
		"nft::filter:input:4",      // empty family
	}
	for _, id := range bad {
		if _, err := nftDeleteArgs(id); err == nil {
			t.Errorf("nftDeleteArgs(%q) = nil error, want error", id)
		}
	}
}
