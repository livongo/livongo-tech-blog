package com.livongo.large_files_blog_post;

import livongo.large_files_blog_post.FileReader;
import livongo.large_files_blog_post.common.DateBucket;
import livongo.large_files_blog_post.common.Result;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class JavaStdLib implements FileReader {
    private JavaStdLib() {
    }

    @Override
    public String description() {
        return "Java StdLib";
    }

    @Override
    public Result consume(Path path) {
        Pattern columnDelimiters = Pattern.compile("\\s*\\|\\s*");
        Pattern nameDelimiter = Pattern.compile(", ");
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            String readLine;
            int lines = 0;

            Set<Integer> indexes = new HashSet<>();
            indexes.add(1);
            indexes.add(433);
            indexes.add(43244);

            Map<Integer, String> specialNames = new HashMap<>();
            Map<DateBucket, Integer> donationsPerYearMonth = new HashMap<>();
            Map<String, Integer> firstNameOccurrences = new HashMap<>();

            while ((readLine = reader.readLine()) != null) {
                lines++;
                String columns[] = columnDelimiters.split(readLine, 9);
                String name = columns[7];
                if (indexes.contains(lines)) {
                    specialNames.put(lines - 1, name);
                }

                String nameParts[] = nameDelimiter.split(name, 3);
                if (nameParts.length > 1) {
                    String firstAndMiddle = nameParts[1].trim();
                    if (!firstAndMiddle.isEmpty()) {
                        String firstNameParts[] = firstAndMiddle.split(" ");
                        firstNameOccurrences.merge(firstNameParts[0].trim(), 1, (a, b) -> a + b);
                    }
                }

                String rawDate = columns[4];
                Integer month = Integer.parseInt(rawDate.substring(4, 6));
                Integer year = Integer.parseInt(rawDate.substring(0, 4));

                donationsPerYearMonth.merge(DateBucket.apply(year, month), 1, (a, b) -> a + b);
            }

            Map.Entry<String, Integer> mostCommonFirstName = Collections.max(
                    firstNameOccurrences.entrySet(),
                    Map.Entry.comparingByValue());

            return Result.fromJava(
                    lines,
                    specialNames,
                    donationsPerYearMonth,
                    mostCommonFirstName.getKey(),
                    mostCommonFirstName.getValue()
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaStdLib implementation = new JavaStdLib();
}
