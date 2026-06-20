// SuperGrep. version 1.0.2

use clap::Parser;
use std::fs::File;
use std::io::{self, BufRead, BufReader};
use std::path::Path;

#[derive(Parser, Debug)]
#[command(version = "1.0.2", about = "Super Grep 1.0.2 is a tool for searching text in indexed files", long_about = None)]
struct Args {
    #[arg(
        short,
        long,
        default_value_t = 4,
        help = "Number of surrounded lines. max 45, default 4. Optional"
    )]
    n: i8,

    #[arg(short, long, default_value_t = String::new(), help = "String to search for. Mandatory")]
    s: String,

    #[arg(short, long, default_value_t = String::new(), conflicts_with = "e", help = "Another string to search for")]
    a: String,

    #[arg(short, long, default_value_t = String::new(), help = "String to exclude from matches. Conflicts with -a")]
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
        eprintln!("Usage: see sg -h");
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
mod tests;
