package main

import (
	"bufio"
	"bytes"
	"fmt"
	"io"
	"os"
	"strings"
	"testing"
)

func TestNormalizeAfterDefaultsInvalidValues(t *testing.T) {
	if normalizeAfter(0) != 4 {
		t.Errorf("normalizeAfter(0) should be 4")
	}
	if normalizeAfter(-1) != 4 {
		t.Errorf("normalizeAfter(-1) should be 4")
	}
	if normalizeAfter(46) != 4 {
		t.Errorf("normalizeAfter(46) should be 4")
	}
}

func TestNormalizeAfterKeepsValidValues(t *testing.T) {
	if normalizeAfter(1) != 1 {
		t.Errorf("normalizeAfter(1) should be 1")
	}
	if normalizeAfter(4) != 4 {
		t.Errorf("normalizeAfter(4) should be 4")
	}
	if normalizeAfter(45) != 45 {
		t.Errorf("normalizeAfter(45) should be 45")
	}
}

func TestLineContainsIgnoresCaseByDefault(t *testing.T) {
	if !lineContains("Container container", "container", false) {
		t.Error("should find 'container' case-insensitively")
	}
	if !lineContains("Container container", "CONTAINER", false) {
		t.Error("should find 'CONTAINER' case-insensitively")
	}
}

func TestLineContainsCaseSensitive(t *testing.T) {
	if !lineContains("Container container", "Container", true) {
		t.Error("should find 'Container' case-sensitively")
	}
	if lineContains("Container container", "CONTAINER", true) {
		t.Error("should NOT find 'CONTAINER' case-sensitively")
	}
}

func TestLineMatchesRequiresBothSearchStrings(t *testing.T) {
	if !lineMatches("Container container = getContentPane();", "container", "getContentPane", "", false) {
		t.Error("should match both strings")
	}
	if lineMatches("Container container = getContentPane();", "container", "missing", "", false) {
		t.Error("should NOT match missing string")
	}
}

func TestLineMatchesAllowsEmptySecondSearchString(t *testing.T) {
	if !lineMatches("Container container = getContentPane();", "container", "", "", false) {
		t.Error("should match with empty second string")
	}
}

func TestLineMatchesExcludesExcludeString(t *testing.T) {
	if !lineMatches("Container container = getContentPane();", "container", "", "missing", false) {
		t.Error("should match when exclude string is not found")
	}
	if lineMatches("Container container = getContentPane();", "container", "", "getContentPane", false) {
		t.Error("should NOT match when exclude string is found")
	}
}

func TestSearchContainerInFilecode(t *testing.T) {
	f, err := os.Open("../filecode")
	if err != nil {
		t.Fatalf("filecode not found: %v", err)
	}
	defer f.Close()

	scanner := bufio.NewScanner(f)
	found := false
	for scanner.Scan() {
		if strings.Contains(scanner.Text(), "container") {
			found = true
			break
		}
	}
	if !found {
		t.Error("Expected to find 'container' in filecode")
	}
}

func TestRunContainerSearch(t *testing.T) {
	old := os.Stdout
	r, w, _ := os.Pipe()
	os.Stdout = w

	args := Args{N: 4, S: "container"}
	err := Run(args, "../filecode")

	w.Close()
	var buf bytes.Buffer
	io.Copy(&buf, r)
	os.Stdout = old

	if err != nil {
		t.Fatalf("Run failed: %v", err)
	}

	output := buf.String()
	if !strings.Contains(output, "15/Lines4Client.java") {
		t.Error("Expected to find filename 15/Lines4Client.java in output")
	}
	if !strings.Contains(output, "* 34") {
		t.Error("Expected to find match marker at line 34")
	}
	if !strings.Contains(output, "* 71") {
		t.Error("Expected to find match marker at line 71")
	}
}

func TestRunContainerSearchExactOutput(t *testing.T) {
	old := os.Stdout
	r, w, _ := os.Pipe()
	os.Stdout = w

	args := Args{N: 4, S: "container"}
	err := Run(args, "../filecode")

	w.Close()
	var buf bytes.Buffer
	io.Copy(&buf, r)
	os.Stdout = old

	if err != nil {
		t.Fatalf("Run failed: %v", err)
	}

	output := buf.String()
	normalized := strings.ReplaceAll(output, "\r\n", "\n")
	actual := strings.Split(normalized, "\n")
	if len(actual) > 0 && actual[len(actual)-1] == "" {
		actual = actual[:len(actual)-1]
	}

	expected := []string{
		"",
		"1",
		"15/Lines4Client.java",
		"",
		"",
		"  31\t   // Set up user-interface and board",
		"  32\t   public void init()",
		"  33\t   {",
		"* 34\t\t  Container container = getContentPane();",
		"  35\t ",
		"  36\t\t  // set up JTextArea to display messages to user",
		"  37\t\t  displayArea = new JTextArea( 5, 30 );",
		"  38\t\t  displayArea.setEditable( false );",
		"* 39\t\t  container.add( new JScrollPane( displayArea ),",
		"  40\t\t\t BorderLayout.SOUTH );",
		"  41\t",
		"  42\t\t  // set up panel for squares in board",
		"  43\t\t  boardPanel = new JPanel();",
		"",
		"  68\t\t  // textfield to display player's mark",
		"  69\t\t  idField = new JTextField();",
		"  70\t\t  idField.setEditable( false );",
		"* 71\t\t  container.add( idField, BorderLayout.NORTH );",
		"  72\t      ",
		"  73\t\t  // set up panel to contain boardPanel (for layout purposes)",
		"  74\t\t  panel2 = new JPanel();",
		"  75\t\t  panel2.add( boardPanel, BorderLayout.CENTER );",
		"* 76\t\t  container.add( panel2, BorderLayout.CENTER );",
		"  77\t\t  ",
		"  78\t\t  setSize( 300, 300 );",
		"  79\t\t  setVisible( true );",
		"  80\t   }",
	}

	if len(actual) != len(expected) {
		t.Fatalf("output line count mismatch:\ngot %d lines\nwant %d lines\n\ngot:\n%s",
			len(actual), len(expected), formatLines(actual))
	}

	for i := range expected {
		if actual[i] != expected[i] {
			t.Errorf("line %d mismatch:\n  got:  %q\n  want: %q", i, actual[i], expected[i])
		}
	}
}

func TestRunWithExclude(t *testing.T) {
	old := os.Stdout
	r, w, _ := os.Pipe()
	os.Stdout = w

	args := Args{N: 4, S: "container", E: "getContentPane"}
	err := Run(args, "../filecode")

	w.Close()
	var buf bytes.Buffer
	io.Copy(&buf, r)
	os.Stdout = old

	if err != nil {
		t.Fatalf("Run failed: %v", err)
	}

	output := buf.String()
	// line 34 contains "getContentPane" so it should be excluded
	if strings.Contains(output, "* 34") {
		t.Error("line 34 should be excluded (contains 'getContentPane')")
	}
	// lines 39, 71, 76 do NOT contain "getContentPane" so they should still match
	if !strings.Contains(output, "* 39") {
		t.Error("line 39 should match (does not contain 'getContentPane')")
	}
	if !strings.Contains(output, "* 71") {
		t.Error("line 71 should match (does not contain 'getContentPane')")
	}
	if !strings.Contains(output, "* 76") {
		t.Error("line 76 should match (does not contain 'getContentPane')")
	}
}

func TestRunWithSecondString(t *testing.T) {
	old := os.Stdout
	r, w, _ := os.Pipe()
	os.Stdout = w

	args := Args{N: 4, S: "container", A: "getContentPane"}
	err := Run(args, "../filecode")

	w.Close()
	var buf bytes.Buffer
	io.Copy(&buf, r)
	os.Stdout = old

	if err != nil {
		t.Fatalf("Run failed: %v", err)
	}

	output := buf.String()
	// line 34 contains both "container" and "getContentPane"
	if !strings.Contains(output, "* 34") {
		t.Error("line 34 should match both strings")
	}
	// line 39 contains "container" but NOT "getContentPane"
	if strings.Contains(output, "* 39") {
		t.Error("line 39 should NOT match (missing 'getContentPane')")
	}
	// line 71 contains "container" but NOT "getContentPane"
	if strings.Contains(output, "* 71") {
		t.Error("line 71 should NOT match (missing 'getContentPane')")
	}
	// line 76 contains "container" but NOT "getContentPane"
	if strings.Contains(output, "* 76") {
		t.Error("line 76 should NOT match (missing 'getContentPane')")
	}
}

func formatLines(lines []string) string {
	var b strings.Builder
	for i, l := range lines {
		fmt.Fprintf(&b, "  %d: %q\n", i, l)
	}
	return b.String()
}
