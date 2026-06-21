package main

import (
	"bufio"
	"flag"
	"fmt"
	"os"
	"strings"
)

type Args struct {
	N int
	S string
	A string
	E string
	C bool
}

func normalizeAfter(n int) int {
	if n <= 0 || n > 45 {
		return 4
	}
	return n
}

func lineContains(line, searchStr string, caseSensitive bool) bool {
	if caseSensitive {
		return strings.Contains(line, searchStr)
	}
	return strings.Contains(strings.ToLower(line), strings.ToLower(searchStr))
}

func lineMatches(line, searchStr, searchStrSecond, searchStrExclude string, caseSensitive bool) bool {
	if !lineContains(line, searchStr, caseSensitive) {
		return false
	}
	if searchStrSecond != "" && !lineContains(line, searchStrSecond, caseSensitive) {
		return false
	}
	if searchStrExclude != "" && lineContains(line, searchStrExclude, caseSensitive) {
		return false
	}
	return true
}

func Run(args Args, dbPath string) error {
	after := normalizeAfter(args.N)
	if args.S == "" {
		fmt.Fprintln(os.Stderr, "Usage: see sg -h")
		return nil
	}
	if args.A != "" && args.E != "" {
		fmt.Fprintln(os.Stderr, "-a and -e are mutually exclusive")
		return nil
	}

	f, err := os.Open(dbPath)
	if err != nil {
		return err
	}
	defer f.Close()

	pwd := "."
	scanner := bufio.NewScanner(f)

	var (
		search       bool
		next         int
		current      int
		index        int
		firstPrint   = true
		lastLine     int
		currentIndex int
		filename     string
	)

	lineS := make([]string, after)

	for scanner.Scan() {
		line := scanner.Text()

		if strings.HasPrefix(line, "_") {
			if !scanner.Scan() {
				break
			}
			filenameLine := scanner.Text()
			parts := strings.Fields(filenameLine)
			filename = ""
			if len(parts) > 0 {
				filename = parts[0]
			}

			if strings.Contains(filename, pwd) {
				search = true
			} else if search {
				break
			} else {
				search = false
			}

			next = 0
			current = 0
			index = 0
			firstPrint = true
			lastLine = 0
		} else if search {
			lineS[index] = line
			index = (index + 1) % after
			current++

			containsNeedle := lineMatches(line, args.S, args.A, args.E, args.C)

			if next > 0 {
				if containsNeedle {
					fmt.Print("*")
					next = after
				} else {
					fmt.Print(" ")
					next--
				}
				fmt.Printf(" %d\t%s\n", current, line)
				lastLine = current
			} else if containsNeedle {
				if firstPrint {
					currentIndex++
					fmt.Printf("\n%d\n%s\n\n", currentIndex, filename)
					firstPrint = false
				}

				if current-after+1 > lastLine {
					fmt.Println()
				}

				i := index
				for offset := -(after - 1); offset < 0; offset++ {
					lineNum := current + offset
					if lineNum >= 1 && lineNum > lastLine {
						fmt.Printf("  %d\t%s\n", lineNum, lineS[i])
					}
					i = (i + 1) % after
				}
				fmt.Printf("* %d\t%s\n", current, line)
				next = after
			}
		}
	}

	return scanner.Err()
}

func main() {
	n := flag.Int("n", 4, "Number of surrounded lines. max 45, default 4. Optional")
	s := flag.String("s", "", "String to search for. Mandatory")
	a := flag.String("a", "", "Another string to search for")
	e := flag.String("e", "", "String to exclude from matches. Conflicts with -a")
	c := flag.Bool("c", false, "Case sensitive search")
	flag.Parse()

	args := Args{
		N: *n,
		S: *s,
		A: *a,
		E: *e,
		C: *c,
	}

	if err := Run(args, "../filecode"); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
}
