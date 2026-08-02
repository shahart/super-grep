const std = @import("std");
const Io = std.Io;

const version = "1.0.2";

const Args = struct {
    n: i8 = 4,
    search: []const u8 = "",
    second: []const u8 = "",
    exclude: []const u8 = "",
    case_sensitive: bool = false,
};

const ParseResult = union(enum) {
    args: Args,
    help,
    version,
};

pub fn main(init: std.process.Init) !void {
    const allocator = init.arena.allocator();
    const argv = try init.minimal.args.toSlice(allocator);

    var stdout_buffer: [4096]u8 = undefined;
    var stdout_file_writer: Io.File.Writer = .init(.stdout(), init.io, &stdout_buffer);
    const stdout = &stdout_file_writer.interface;
    defer stdout.flush() catch {};

    const parsed = parseArgs(argv) catch |err| {
        std.debug.print("sg: {s}\nUsage: see sg -h\n", .{parseErrorMessage(err)});
        return;
    };

    const args = switch (parsed) {
        .help => {
            try printHelp(stdout);
            return;
        },
        .version => {
            try stdout.print("SuperGrep {s}\n", .{version});
            return;
        },
        .args => |args| args,
    };

    if (args.search.len == 0) {
        std.debug.print("Usage: see sg -h\n", .{});
        return;
    }

    var file = try Io.Dir.cwd().openFile(init.io, "filecode", .{});
    defer file.close(init.io);

    var read_buffer: [8192]u8 = undefined;
    var file_reader = file.reader(init.io, &read_buffer);
    const contents = try file_reader.interface.allocRemaining(allocator, .unlimited);

    try searchFile(contents, args, stdout);
}

fn parseArgs(argv: []const []const u8) !ParseResult {
    var args = Args{};
    var i: usize = 1;
    while (i < argv.len) : (i += 1) {
        const arg = argv[i];

        if (std.mem.eql(u8, arg, "-h") or std.mem.eql(u8, arg, "--help")) return .help;
        if (std.mem.eql(u8, arg, "-V") or std.mem.eql(u8, arg, "--version")) return .version;
        if (std.mem.eql(u8, arg, "-c") or std.mem.eql(u8, arg, "--c")) {
            args.case_sensitive = true;
            continue;
        }

        const option = optionValue(arg) orelse return error.UnknownArgument;
        const value = if (option.inline_value) |value|
            value
        else value: {
            i += 1;
            if (i >= argv.len) return error.MissingValue;
            break :value argv[i];
        };

        switch (option.kind) {
            .n => args.n = std.fmt.parseInt(i8, value, 10) catch return error.InvalidNumber,
            .search => args.search = value,
            .second => args.second = value,
            .exclude => args.exclude = value,
        }
    }

    if (args.second.len != 0 and args.exclude.len != 0) return error.ConflictingArguments;
    return .{ .args = args };
}

const OptionKind = enum { n, search, second, exclude };

const Option = struct {
    kind: OptionKind,
    inline_value: ?[]const u8,
};

fn optionValue(arg: []const u8) ?Option {
    const names = [_]struct {
        short: []const u8,
        long: []const u8,
        kind: OptionKind,
    }{
        .{ .short = "-n", .long = "--n", .kind = .n },
        .{ .short = "-s", .long = "--s", .kind = .search },
        .{ .short = "-a", .long = "--a", .kind = .second },
        .{ .short = "-e", .long = "--e", .kind = .exclude },
    };

    for (names) |name| {
        if (std.mem.eql(u8, arg, name.short) or std.mem.eql(u8, arg, name.long))
            return .{ .kind = name.kind, .inline_value = null };

        if (std.mem.startsWith(u8, arg, name.long) and arg.len > name.long.len and arg[name.long.len] == '=')
            return .{ .kind = name.kind, .inline_value = arg[name.long.len + 1 ..] };

        if (std.mem.startsWith(u8, arg, name.short) and arg.len > name.short.len)
            return .{ .kind = name.kind, .inline_value = arg[name.short.len..] };
    }
    return null;
}

fn parseErrorMessage(err: anyerror) []const u8 {
    return switch (err) {
        error.UnknownArgument => "unknown argument",
        error.MissingValue => "option requires a value",
        error.InvalidNumber => "-n must be an integer between -128 and 127",
        error.ConflictingArguments => "-a cannot be used with -e",
        else => "invalid arguments",
    };
}

fn printHelp(writer: *Io.Writer) !void {
    try writer.writeAll(
        \\Super Grep 1.0.2 is a tool for searching text in indexed files
        \\
        \\Usage: sg [OPTIONS]
        \\
        \\Options:
        \\  -n, --n <N>  Number of surrounded lines. max 45, default 4. Optional
        \\  -s, --s <S>  String to search for. Mandatory
        \\  -a, --a <A>  Another string to search for
        \\  -e, --e <E>  String to exclude from matches. Conflicts with -a
        \\  -c, --c      Case sensitive search
        \\  -h, --help   Print help
        \\  -V, --version  Print version
        \\
    );
}

fn normalizeAfter(n: i8) usize {
    if (n <= 0 or n > 45) return 4;
    return @intCast(n);
}

fn lineContains(line: []const u8, needle: []const u8, case_sensitive: bool) bool {
    if (needle.len == 0) return true;
    if (needle.len > line.len) return false;
    if (case_sensitive) return std.mem.indexOf(u8, line, needle) != null;

    var i: usize = 0;
    while (i + needle.len <= line.len) : (i += 1) {
        if (std.ascii.eqlIgnoreCase(line[i..][0..needle.len], needle)) return true;
    }
    return false;
}

fn lineMatches(line: []const u8, args: Args) bool {
    return lineContains(line, args.search, args.case_sensitive) and
        (args.second.len == 0 or lineContains(line, args.second, args.case_sensitive)) and
        (args.exclude.len == 0 or !lineContains(line, args.exclude, args.case_sensitive));
}

fn searchFile(contents: []const u8, args: Args, writer: *Io.Writer) !void {
    const after = normalizeAfter(args.n);
    var lines = std.mem.splitScalar(u8, contents, '\n');

    var search = false;
    var next: usize = 0;
    var current: usize = 0;
    var index: usize = 0;
    var first_print = true;
    var last_line: usize = 0;
    var current_index: usize = 0;
    var filename: []const u8 = "";
    var line_buffer: [45][]const u8 = @splat("");

    while (lines.next()) |raw_line| {
        const line = std.mem.trimEnd(u8, raw_line, "\r");
        if (std.mem.startsWith(u8, line, "_")) {
            const filename_line = lines.next() orelse break;
            var words = std.mem.tokenizeAny(u8, std.mem.trimEnd(u8, filename_line, "\r"), " \t");
            filename = words.next() orelse "";

            if (std.mem.indexOf(u8, filename, ".") != null) {
                search = true;
            } else if (search) {
                break;
            } else {
                search = false;
            }

            next = 0;
            current = 0;
            index = 0;
            first_print = true;
            last_line = 0;
        } else if (search) {
            line_buffer[index] = line;
            index = (index + 1) % after;
            current += 1;

            const matches = lineMatches(line, args);
            if (next > 0) {
                if (matches) {
                    try writer.writeByte('*');
                    next = after;
                } else {
                    try writer.writeByte(' ');
                    next -= 1;
                }
                try writer.print(" {d}\t{s}\n", .{ current, line });
                last_line = current;
            } else if (matches) {
                if (first_print) {
                    current_index += 1;
                    try writer.print("\n{d}\n{s}\n\n", .{ current_index, filename });
                    first_print = false;
                }

                if (current > after and current - after + 1 > last_line) try writer.writeByte('\n');

                const previous_count = @min(after - 1, current - 1);
                var ring_index = (index + after - previous_count - 1) % after;
                var line_num = current - previous_count;
                while (line_num < current) : (line_num += 1) {
                    if (line_num > last_line)
                        try writer.print("  {d}\t{s}\n", .{ line_num, line_buffer[ring_index] });
                    ring_index = (ring_index + 1) % after;
                }
                try writer.print("* {d}\t{s}\n", .{ current, line });
                next = after;
            }
        }
    }
}

test "matching honors case, second, and exclude options" {
    try std.testing.expect(lineMatches("Alpha beta", .{ .search = "alpha" }));
    try std.testing.expect(!lineMatches("Alpha beta", .{ .search = "alpha", .case_sensitive = true }));
    try std.testing.expect(lineMatches("Alpha beta", .{ .search = "alpha", .second = "BETA" }));
    try std.testing.expect(!lineMatches("Alpha beta", .{ .search = "alpha", .exclude = "beta" }));
}

test "context count is normalized" {
    try std.testing.expectEqual(@as(usize, 4), normalizeAfter(0));
    try std.testing.expectEqual(@as(usize, 4), normalizeAfter(-1));
    try std.testing.expectEqual(@as(usize, 4), normalizeAfter(46));
    try std.testing.expectEqual(@as(usize, 12), normalizeAfter(12));
}
