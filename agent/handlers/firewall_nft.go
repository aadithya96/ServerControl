package handlers

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
	"strings"
)

// nftables exposes its ruleset as JSON via `nft -j list ruleset`. We map that
// onto the same FirewallChain / FirewallRuleItem DTOs used for iptables so the
// Android UI does not need to know which backend is in use (other than the
// "backend" field on the response).
//
// Rule deletion in nftables is by stable handle rather than line number, so we
// encode enough routing information into the rule ID:
//
//	nft:<family>:<table>:<chain>:<handle>

type nftChain struct {
	Family string `json:"family"`
	Table  string `json:"table"`
	Name   string `json:"name"`
	Handle int    `json:"handle"`
	Policy string `json:"policy"`
}

type nftRule struct {
	Family string                       `json:"family"`
	Table  string                       `json:"table"`
	Chain  string                       `json:"chain"`
	Handle int                          `json:"handle"`
	Expr   []map[string]json.RawMessage `json:"expr"`
}

// parseNftables parses `nft -j list ruleset` output into ordered chains.
func parseNftables(data string) []FirewallChain {
	var top struct {
		Nftables []map[string]json.RawMessage `json:"nftables"`
	}
	if err := json.Unmarshal([]byte(data), &top); err != nil {
		return nil
	}

	var chains []FirewallChain
	index := make(map[string]int) // family/table/name -> position in chains

	// First pass: declare chains in the order they appear.
	for _, obj := range top.Nftables {
		raw, ok := obj["chain"]
		if !ok {
			continue
		}
		var c nftChain
		if json.Unmarshal(raw, &c) != nil {
			continue
		}
		key := c.Family + "/" + c.Table + "/" + c.Name
		if _, exists := index[key]; !exists {
			index[key] = len(chains)
			chains = append(chains, FirewallChain{
				Name:   c.Name,
				Policy: c.Policy,
				Rules:  []FirewallRuleItem{},
			})
		}
	}

	// Second pass: attach rules to their chains.
	for _, obj := range top.Nftables {
		raw, ok := obj["rule"]
		if !ok {
			continue
		}
		var ru nftRule
		if json.Unmarshal(raw, &ru) != nil {
			continue
		}
		key := ru.Family + "/" + ru.Table + "/" + ru.Chain
		idx, exists := index[key]
		if !exists {
			index[key] = len(chains)
			chains = append(chains, FirewallChain{Name: ru.Chain, Rules: []FirewallRuleItem{}})
			idx = index[key]
		}
		chains[idx].Rules = append(chains[idx].Rules, nftRuleToItem(ru))
	}

	return chains
}

func nftRuleToItem(ru nftRule) FirewallRuleItem {
	item := FirewallRuleItem{
		ID:  fmt.Sprintf("nft:%s:%s:%s:%d", ru.Family, ru.Table, ru.Chain, ru.Handle),
		Num: ru.Handle,
	}

	var opts []string
	for _, stmt := range ru.Expr {
		if raw, ok := stmt["match"]; ok {
			parseNftMatch(raw, &item, &opts)
			continue
		}
		if raw, ok := stmt["counter"]; ok {
			var c struct {
				Packets int64 `json:"packets"`
				Bytes   int64 `json:"bytes"`
			}
			if json.Unmarshal(raw, &c) == nil {
				item.Packets = c.Packets
				item.Bytes = c.Bytes
			}
			continue
		}
		// Verdict statements.
		for _, verdict := range []string{"accept", "drop", "reject", "return", "queue", "continue"} {
			if _, ok := stmt[verdict]; ok {
				item.Target = strings.ToUpper(verdict)
			}
		}
		if raw, ok := stmt["jump"]; ok {
			item.Target = "JUMP " + nftVerdictTarget(raw)
		}
		if raw, ok := stmt["goto"]; ok {
			item.Target = "GOTO " + nftVerdictTarget(raw)
		}
	}

	item.Options = strings.Join(opts, " ")
	if item.Target == "" {
		item.Target = "—"
	}
	return item
}

func nftVerdictTarget(raw json.RawMessage) string {
	var v struct {
		Target string `json:"target"`
	}
	json.Unmarshal(raw, &v)
	return v.Target
}

func parseNftMatch(raw json.RawMessage, item *FirewallRuleItem, opts *[]string) {
	var m struct {
		Left  json.RawMessage `json:"left"`
		Right json.RawMessage `json:"right"`
	}
	if json.Unmarshal(raw, &m) != nil {
		return
	}

	var left struct {
		Payload *struct {
			Protocol string `json:"protocol"`
			Field    string `json:"field"`
		} `json:"payload"`
		Meta *struct {
			Key string `json:"key"`
		} `json:"meta"`
	}
	json.Unmarshal(m.Left, &left)
	right := nftValueToString(m.Right)

	switch {
	case left.Payload != nil:
		switch left.Payload.Field {
		case "saddr":
			item.Source = right
		case "daddr":
			item.Destination = right
		case "dport":
			if item.Protocol == "" {
				item.Protocol = left.Payload.Protocol
			}
			*opts = append(*opts, "dport "+right)
		case "sport":
			if item.Protocol == "" {
				item.Protocol = left.Payload.Protocol
			}
			*opts = append(*opts, "sport "+right)
		default:
			*opts = append(*opts, left.Payload.Field+" "+right)
		}
	case left.Meta != nil:
		if left.Meta.Key == "l4proto" || left.Meta.Key == "nfproto" {
			if item.Protocol == "" {
				item.Protocol = right
			}
		} else {
			*opts = append(*opts, left.Meta.Key+" "+right)
		}
	}
}

// nftValueToString renders an nftables JSON value (number, string, prefix, or
// set) as a human-readable string.
func nftValueToString(raw json.RawMessage) string {
	if len(raw) == 0 {
		return ""
	}
	dec := json.NewDecoder(bytes.NewReader(raw))
	dec.UseNumber()
	var v interface{}
	if dec.Decode(&v) != nil {
		return strings.Trim(string(raw), `"`)
	}

	switch val := v.(type) {
	case string:
		return val
	case json.Number:
		return val.String()
	case map[string]interface{}:
		if p, ok := val["prefix"].(map[string]interface{}); ok {
			addr, _ := p["addr"].(string)
			length := ""
			if l, ok := p["len"].(json.Number); ok {
				length = l.String()
			}
			return addr + "/" + length
		}
		if set, ok := val["set"].([]interface{}); ok {
			parts := make([]string, 0, len(set))
			for _, e := range set {
				parts = append(parts, fmt.Sprintf("%v", e))
			}
			return "{" + strings.Join(parts, ",") + "}"
		}
		return fmt.Sprintf("%v", val)
	default:
		return fmt.Sprintf("%v", val)
	}
}

// nftDeleteArgs builds the `nft` argument list to delete the rule identified by
// an "nft:<family>:<table>:<chain>:<handle>" rule ID.
func nftDeleteArgs(ruleID string) ([]string, error) {
	parts := strings.Split(ruleID, ":")
	if len(parts) != 5 || parts[0] != "nft" {
		return nil, fmt.Errorf("invalid nftables rule_id: %q", ruleID)
	}
	family, table, chain, handle := parts[1], parts[2], parts[3], parts[4]
	if _, err := strconv.Atoi(handle); err != nil {
		return nil, fmt.Errorf("invalid handle in rule_id: %q", handle)
	}
	if family == "" || table == "" || chain == "" {
		return nil, fmt.Errorf("incomplete nftables rule_id: %q", ruleID)
	}
	return []string{"delete", "rule", family, table, chain, "handle", handle}, nil
}
