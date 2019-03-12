#!/bin/bash -e

DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" &>/dev/null && pwd)
TRIALS_PER_ROUND=10
TOTAL_ROUNDS=10
TEST_FILE="$1"
OUTPUT_FILE='measurements.csv'

compile-binary () (
    sbt pack
)

normalize-io-cache () {
    echo >&2 'Normalizing the IO cache by running one of the rounds and discarding the results'
    "$DIR"/target/pack/bin/main process -l fs2-core -f "$TEST_FILE" > /dev/null
}

implementations-in-random-order () {
    "$DIR/"target/pack/bin/main print-libraries | shuf -
}

print-progress-header () {
    echo >&2 "Processing $TEST_FILE into $OUTPUT_FILE"
    echo >&2 ' Round | Implementation    | Test  '
    echo >&2 '-------+-------------------+-------'
}

update-progress-line () {
    local round="$1" index="$2" cmd="$3" trial="$4"
    printf '\r %2d/%2d | %d/%d: %-12s | %2d/%2d ' \
           "$round" "$TOTAL_ROUNDS" \
           "$index" "$IMPLEMENTATION_COUNT" "$cmd" \
           "$trial" "$TRIALS_PER_ROUND"

}

compile-binary
normalize-io-cache
IMPLEMENTATION_COUNT=$(implementations-in-random-order | wc -l)
print-progress-header
seq 1 "$TOTAL_ROUNDS" |
    while read round_index; do
        cmd_index=0
        implementations-in-random-order |
            while ((cmd_index++)); read cmd
            do
                seq 1 "$TRIALS_PER_ROUND" |
                    while read trial
                    do
                        update-progress-line \
                            "$round_index" \
                            "$cmd_index" \
                            "$cmd" \
                            "$trial"
                        "$DIR"/target/pack/bin/main \
                              process \
                              -l "$cmd" \
                              -f "$TEST_FILE" >> "$OUTPUT_FILE"
                    done
            done
    done

echo
