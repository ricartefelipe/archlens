package io.archlens.analyzer.cli;

import io.archlens.analyzer.cli.command.ScanCommand;
import io.archlens.analyzer.cli.command.ValidateCommand;
import io.archlens.analyzer.cli.command.VersionCommand;
import io.archlens.analyzer.core.analysis.ProjectAnalyzer;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(
        name = "archlens",
        mixinStandardHelpOptions = true,
        versionProvider = VersionCommand.class,
        subcommands = {
                ScanCommand.class,
                ValidateCommand.class,
                VersionCommand.class
        },
        description = "ArchLens static analyzer for legacy Java applications"
)
public final class ArchLensCli implements Runnable {

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Show help")
    boolean help;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new ArchLensCli()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        CommandLine.usage(this, System.out);
    }

    public static String analyzerVersion() {
        return ProjectAnalyzer.ANALYZER_VERSION;
    }
}
