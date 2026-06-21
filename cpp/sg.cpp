// SuperGrep. fast "kg". version 1.01
// modification history in "sg-history"

#include <algorithm>
#include <array>
#include <cctype>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <string>
#include <unistd.h>
#include <vector>

namespace {

constexpr int MaxLine = 200;
constexpr int MaxAfter = 45;
constexpr int DefaultAfter = 4;

std::string uppercase(std::string value)
{
    std::transform(value.begin(), value.end(), value.begin(), [](unsigned char ch) {
        return static_cast<char>(std::toupper(ch));
    });
    return value;
}

bool contains(const std::string& haystack, const std::string& needle, bool case_sensitive)
{
    if (case_sensitive) {
        return haystack.find(needle) != std::string::npos;
    }

    return uppercase(haystack).find(uppercase(needle)) != std::string::npos;
}

std::string shell_quote(const std::string& value)
{
    std::string quoted = "'";
    for (char ch : value) {
        if (ch == '\'') {
            quoted += "'\\''";
        } else {
            quoted += ch;
        }
    }
    quoted += "'";
    return quoted;
}

class PipeReader {
public:
    explicit PipeReader(const std::string& path)
    {
        std::string command = "gzip -cdf " + shell_quote(path);
        pipe_ = popen(command.c_str(), "r");
    }

    ~PipeReader()
    {
        if (pipe_ != nullptr) {
            pclose(pipe_);
        }
    }

    PipeReader(const PipeReader&) = delete;
    PipeReader& operator=(const PipeReader&) = delete;

    bool ok() const
    {
        return pipe_ != nullptr;
    }

    bool getline(std::string& line)
    {
        if (pipe_ == nullptr) {
            return false;
        }

        std::array<char, MaxLine> buffer {};
        if (std::fgets(buffer.data(), static_cast<int>(buffer.size()), pipe_) == nullptr) {
            return false;
        }

        line = buffer.data();
        return true;
    }

private:
    FILE* pipe_ = nullptr;
};

void print_usage()
{
    std::printf("Super Grep v1.01\n");
    std::printf("Usage: sg [-n] [-c] [-a|-n] [-v] string [string2]\n");
    std::printf("           -n Number of surrounded lines. max 45, default 4\n");
    std::printf("           -c Case sensitive search\n");
    std::printf("           -a search string And string2\n");
    std::printf("           -n search string Not string2\n");
    std::printf("           -h show modification History\n");
}

bool line_matches(
    const std::string& line,
    const std::string& primary,
    const std::string& secondary,
    bool and_search,
    bool not_search,
    bool case_sensitive)
{
    if (not_search) {
        return contains(line, secondary, case_sensitive) && !contains(line, primary, case_sensitive);
    }

    if (!contains(line, primary, case_sensitive)) {
        return false;
    }

    return !and_search || contains(line, secondary, case_sensitive);
}

} // namespace

int main(int argc, char* argv[])
{
    char c = 0;
    int after = 0;
    bool and_search = false;
    bool not_search = false;
    bool case_sensitive = false;

    while (--argc > 0 && (*++argv)[0] == '-') {
        while ((c = *++argv[0]) != '\0') {
            switch (std::toupper(static_cast<unsigned char>(c))) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':
                after = after * 10 + c - '0';
                break;

            case 'C':
                if (case_sensitive) {
                    std::printf("sg: not twice %c\n", c);
                    return 0;
                }
                case_sensitive = true;
                break;

            case 'A':
                if (and_search || not_search) {
                    std::printf("sg: not twice %c\n", c);
                    return 0;
                }
                and_search = true;
                break;

            case 'N':
                if (not_search || and_search) {
                    std::printf("sg: not twice %c\n", c);
                    return 0;
                }
                not_search = true;
                break;

            case 'H':
                std::system("cat ~/sg-history");
                return 0;

            default:
                std::printf("sg: illegal option %c\n", c);
                argc = 0;
                break;
            }
        }
    }

    if (after <= 0 || after > MaxAfter) {
        after = DefaultAfter;
    }

    if (argc != 1 + static_cast<int>(and_search) + static_cast<int>(not_search)) {
        print_usage();
        return 0;
    }

    std::string secondary;
    if (and_search || not_search) {
        secondary = *argv;
        ++argv;
    }
    std::string primary = *argv;

    const char* env_db = std::getenv("SG_DATABASE");
    std::string db = (env_db != nullptr && env_db[0] != '\0') ? env_db : "/repo/mine";
    PipeReader input(db + "/filecode");
    if (!input.ok()) {
        std::perror("sg: filecode");
        return 1;
    }

    std::array<char, MaxLine> cwd {};
    std::string pwd = ".";
    if (getcwd(cwd.data(), cwd.size()) != nullptr) {
        char* basename = std::strrchr(cwd.data(), '/');
        pwd = basename == nullptr ? cwd.data() : basename + 1;
    }

    bool print_relative_header = pwd.find("_lib") != std::string::npos || pwd.find("main") != std::string::npos;
    if (!print_relative_header) {
        pwd = ".";
    } else {
        std::printf("Searching in %s\n", pwd.c_str());
    }

    bool search = false;
    int next = 0;
    int current = 0;
    int index = 0;
    bool first_print = true;
    int last_line = 0;
    int current_index = 0;
    std::string filename;
    std::vector<std::string> line_buffer(static_cast<size_t>(after));

    std::string line;
    while (input.getline(line)) {
        if (!line.empty() && line[0] == '_') {
            if (!input.getline(line)) {
                break;
            }

            size_t name_end = line.find_first_of(" \t\r\n");
            filename = line.substr(0, name_end);

            if (filename.find(pwd) != std::string::npos) {
                search = true;
            } else if (search) {
                return 0;
            } else {
                search = false;
            }

            next = 0;
            current = 0;
            index = 0;
            first_print = true;
            last_line = 0;
            continue;
        }

        if (!search) {
            continue;
        }

        line_buffer[static_cast<size_t>(index)] = line;
        index = (index + 1) % after;
        ++current;

        bool matched = line_matches(line, primary, secondary, and_search, not_search, case_sensitive);

        if (next > 0) {
            if (matched) {
                std::printf("*");
                next = after;
            } else {
                std::printf(" ");
                --next;
            }
            std::printf(" %d\t%s", current, line.c_str());
            last_line = current;
            continue;
        }

        if (!matched) {
            continue;
        }

        if (first_print) {
            ++current_index;

            std::string header = filename;
            if (print_relative_header) {
                size_t slash = filename.find('/');
                if (slash != std::string::npos) {
                    header = filename.substr(slash + 1);
                }
            }

            std::printf("\n%d\n%s\n\n", current_index, header.c_str());
            first_print = false;
        }

        if (current - after + 1 > last_line) {
            std::printf("\n");
        }

        int i = index;
        for (int offset = -(after - 1); offset < 0; ++offset) {
            int line_number = current + offset;
            if (line_number >= 1 && line_number > last_line) {
                std::printf("  %d\t%s", line_number, line_buffer[static_cast<size_t>(i)].c_str());
            }
            i = (i + 1) % after;
        }

        std::printf("* %d\t%s", current, line.c_str());
        next = after;
    }

    return 0;
}
