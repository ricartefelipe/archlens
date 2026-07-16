package io.archlens.analyzer.cli.command;

import io.archlens.analyzer.cli.ArchLensCli;
import picocli.CommandLine.Command;
import picocli.CommandLine.IVersionProvider;

import java.util.concurrent.Callable;

@Command(name = "version", description = "Print analyzer version")
public final class VersionCommand implements Callable<Integer>, IVersionProvider {

    @Override
    public Integer call() {
        System.out.println(ArchLensCli.analyzerVersion());
        return 0;
    }

    @Override
    public String[] getVersion() {
        return new String[]{ArchLensCli.analyzerVersion()};
    }
}
