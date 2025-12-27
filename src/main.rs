// SuperGrep. version 1.00

use std::env;
use std::fs::File;
use std::path::Path;
use std::io::{self, BufRead, BufReader};

fn main() -> io::Result<()> {
    let args: Vec<String> = env::args().skip(1).collect();

    let after = 4; // The number of lines to show after a match. TODO support

    if args.len() != 1 {
        eprintln!("Super Grep v1.00");
        eprintln!("Usage: sg string");
        return Ok(());
    }

    // TODO support args, like -n -a -c

    // Open filecode. TODO with zcat
    let f = BufReader::new(File::open(Path::new("filecode"))?);
    let pwd = ".".to_string();

    // Prepare search strings
    let mut argv_iter = args.iter();

    let search_str = argv_iter.next().unwrap().clone();

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

            let contains_needle = line.contains(&search_str);

            if next > 0 {
                if contains_needle
                {
                    print!("*");
                    next = after;
                } else {
                    print!(" ");
                    next -= 1;
                }
                println!(" {}\t{}", current, line);
                last_line = current;
            } else if contains_needle
            {
                if first_print {
                    current_index += 1;
                    println!(
                        "\n{}\n{}\n",
                        current_index,
                        &filename
                    );
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
    use std::fs::File;
    use std::io::{BufRead, BufReader};

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
}