package config

import (
	"reflect"
	"testing"
)

func TestParseClientIDs(t *testing.T) {
	cases := []struct {
		name   string
		csv    string
		single string
		want   []string
	}{
		{"csv wins, trims spaces", " web , ios ", "legacy", []string{"web", "ios"}},
		{"fallback to single when csv empty", "", "legacy", []string{"legacy"}},
		{"single csv value", "only", "", []string{"only"}},
		{"both empty -> nil", "", "", nil},
		{"csv with empty parts dropped", "a,,b,", "x", []string{"a", "b"}},
	}
	for _, c := range cases {
		t.Run(c.name, func(t *testing.T) {
			got := parseClientIDs(c.csv, c.single)
			if !reflect.DeepEqual(got, c.want) {
				t.Fatalf("parseClientIDs(%q,%q) = %#v, want %#v", c.csv, c.single, got, c.want)
			}
		})
	}
}
