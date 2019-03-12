# Using Scala to Read Really, Really Large Files

This is the companion repo to [this article](posts/0.introduction.md) about reading large
files in Scala, and provides the tooling needed to reproduce our
results.

# Getting the Data Set

While the code assumes a data format, the tooling doesn't hard code the
file location, and it's large enough that it's not committed to source
control.

The US Federal Elections Commission provides a [zip file](https://www.fec.gov/files/bulk-downloads/2018/indiv18.zip)
containing the data set we used (`itcont.txt`), as well as several
smaller files.

# Running Manual Tests

To avoid spinning up `sbt` each time, a runner is created using
[`sbt-pack`](https://github.com/xerial/sbt-pack):

```bash
$ sbt pack packCopyDependencies
```

This creates a runner in the `target/pack` directory, which can be run
as a standard shell script:

```bash
$ ./target/pack/bin/main process -l library -p file-path
```

By default, only timing information is printed to standard out. To check
the correctness of the results, pass `--results` as an additional argument.

# Running Automated Tests

Depending on the size of the file, this can take several hours. The
script takes care of all the plumbing, and takes the data file as it's
sole argument:

```bash
$ ./generate-results.sh file-path
```

Of note: this scripts does not empty the results file
(`measurements.csv`), and will happily append to an older set of
measurements.