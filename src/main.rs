// SuperGrep. version 1.00

use clap::Parser;
use std::fs::File;
use std::io::{self, BufRead, BufReader};
use std::path::Path;

#[derive(Parser, Debug)]
#[command(version = "1.00", about = "Super Grep 1.00 is a tool for searching text in files", long_about = None)]
struct Args {
    #[arg(
        short,
        long,
        default_value_t = 4,
        help = "Number of surrounded lines. max 45, default 4"
    )]
    n: i8,

    #[arg(short, long, default_value_t = String::new(), help = "String to search for")]
    s: String,

    #[arg(short, long, default_value_t = String::new(), conflicts_with = "e", help = "Another string to search for")]
    a: String,

    #[arg(short, long, default_value_t = String::new(), help = "String to exclude from matches")]
    e: String,

    #[arg(short, long, default_value_t = false, help = "Case sensitive search")]
    c: bool,
}

fn normalize_after(n: i8) -> usize {
    let mut after = n as usize;

    if after == 0 || after > 45 {
        after = 4;
    }

    after
}

fn line_contains(line: &str, search_str: &str, case_sensitive: bool) -> bool {
    if case_sensitive {
        line.contains(search_str)
    } else {
        line.to_lowercase().contains(&search_str.to_lowercase())
    }
}

fn line_matches(
    line: &str,
    search_str: &str,
    search_str_second: &str,
    search_str_exclude: &str,
    case_sensitive: bool,
) -> bool {
    line_contains(line, search_str, case_sensitive)
        && (search_str_second.is_empty() || line_contains(line, search_str_second, case_sensitive))
        && (search_str_exclude.is_empty()
            || !line_contains(line, search_str_exclude, case_sensitive))
}

fn main() -> io::Result<()> {
    let args = Args::parse();

    let after = normalize_after(args.n); // The number of lines to show after a match.

    if args.s == "" {
        eprintln!("Super Grep v1.0.2");
        eprintln!("Usage: sg [-n 4] [-c] -s string [-a string] [-e string]");
        eprintln!("           -n Number of surrounded lines. max 45, default 4");
        eprintln!("           -a Another string to search for");
        eprintln!("           -e String to exclude from matches. Cannot be used with -a");
        eprintln!("           -c Case sensitive search");
        return Ok(());
    }

    // TODO support args -e(xclude)

    // Open filecode. TODO with zcat
    let f = BufReader::new(File::open(Path::new("filecode"))?);
    let pwd = ".".to_string();

    let search_str = args.s.clone();
    let search_str_second = args.a.clone();
    let search_str_exclude = args.e.clone();

    // Variables for processing
    let mut search = false;
    let mut next = 0;
    let mut current = 0;
    let mut index = 0;
    let mut first_print = true;
    let mut last_line = 0;
    let mut current_index = 0;
    let mut oldfilename = String::new();
    let mut filename = String::new();

    // Circular buffer for lines
    let mut line_s = vec![String::new(); after];

    // Create iterators for both files
    let mut f_lines = f.lines();

    while let Some(Ok(line)) = f_lines.next() {
        // eprintln!("line {}", line);
        if line.starts_with('_') {
            oldfilename.clear();

            // Read next lines for filename
            let line = match f_lines.next() {
                Some(Ok(l)) => l,
                _ => break,
            };

            filename = line.split_whitespace().next().unwrap_or("").to_string();

            if filename.contains(&pwd) {
                search = true;
            } else if search {
                break;
            } else {
                search = false;
            }

            next = 0;
            current = 0;
            index = 0;
            first_print = true;
            last_line = 0;
        } else if search {
            line_s[index] = line.clone();
            index = (index + 1) % after;
            current += 1;

            let contains_needle = line_matches(
                &line,
                &search_str,
                &search_str_second,
                &search_str_exclude,
                args.c,
            );

            if next > 0 {
                if contains_needle {
                    print!("*");
                    next = after;
                } else {
                    print!(" ");
                    next -= 1;
                }
                println!(" {}\t{}", current, line);
                last_line = current;
            } else if contains_needle {
                if first_print {
                    current_index += 1;
                    println!("\n{}\n{}\n", current_index, &filename);
                    oldfilename = filename.clone();
                    first_print = false;
                }

                if next == 0 {
                    if current - after + 1 > last_line {
                        println!();
                    }

                    let mut i = index;
                    for offset in (-(after as isize - 1))..0 {
                        let line_num = current as isize + offset;
                        if line_num >= 1 && line_num > last_line as isize {
                            println!("  {}\t{}", line_num, line_s[i]);
                        }
                        i = (i + 1) % after;
                    }
                    println!("* {}\t{}", current, line);
                    next = after;
                }
            }
        }
    }

    Ok(())
}

#[cfg(test)]
mod tests {
    use super::{line_contains, line_matches, normalize_after};
    use std::fs::File;
    use std::io::{BufRead, BufReader};

    #[test]
    fn test_normalize_after_defaults_invalid_values() {
        assert_eq!(normalize_after(0), 4);
        assert_eq!(normalize_after(-1), 4);
        assert_eq!(normalize_after(46), 4);
    }

    #[test]
    fn test_normalize_after_keeps_valid_values() {
        assert_eq!(normalize_after(1), 1);
        assert_eq!(normalize_after(4), 4);
        assert_eq!(normalize_after(45), 45);
    }

    #[test]
    fn test_line_matches_ignores_case_by_default() {
        assert!(line_contains("Container container", "container", false));
        assert!(line_contains("Container container", "CONTAINER", false));
    }

    #[test]
    fn test_line_matches_case_sensitive() {
        assert!(line_contains("Container container", "Container", true));
        assert!(!line_contains("Container container", "CONTAINER", true));
    }

    #[test]
    fn test_line_matches_requires_both_search_strings() {
        assert!(line_matches(
            "Container container = getContentPane();",
            "container",
            "getContentPane",
            "",
            false
        ));
        assert!(!line_matches(
            "Container container = getContentPane();",
            "container",
            "missing",
            "",
            false
        ));
    }

    #[test]
    fn test_line_matches_allows_empty_second_search_string() {
        assert!(line_matches(
            "Container container = getContentPane();",
            "container",
            "",
            "",
            false
        ));
    }

    #[test]
    fn test_line_matches_excludes_second_search_string() {
        assert!(line_matches(
            "Container container = getContentPane();",
            "container",
            "",
            "missing",
            false
        ));
        assert!(!line_matches(
            "Container container = getContentPane();",
            "container",
            "",
            "getContentPane",
            false
        ));
    }

    #[test]
    fn test_search_container_in_filecode() {
        let f = File::open("filecode").expect("filecode not found");
        let reader = BufReader::new(f);
        let mut found = false;
        for line in reader.lines() {
            let l = line.expect("read error");
            if l.contains("container") {
                found = true;
                break;
            }
        }
        assert!(found, "Expected to find 'container' in filecode");
    }

    #[test]
    #[ignore]
    fn test_main_returns_container_context_lines() {
        let output = std::process::Command::new("sg.exe")
            .args(["-n", "4", "-s", "container"])
            .output()
            .expect("failed to run sg");

        assert!(
            output.status.success(),
            "sg returned a non-zero exit status"
        );

        let stdout = String::from_utf8(output.stdout).expect("stdout was not valid UTF-8");
        let normalized = stdout.replace("\r\n", "\n");
        let mut actual: Vec<&str> = normalized.split('\n').collect();
        if actual.last() == Some(&"") {
            actual.pop();
        }

        let expected = vec![
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
        ];

        assert_eq!(actual, expected);
    }
}
