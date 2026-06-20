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
