/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.extensions.manual.parser;

import org.jboss.aesh.console.Config;
import org.jboss.aesh.extensions.manual.ManPage;
import org.jboss.aesh.extensions.page.PageLoader;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Read a asciidoc file and parse it to something that can be
 * displayed nicely in a terminal.
 *
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ManPageLoader implements PageLoader {

    private List<ManSection> sections;
    private File file;
    private String name;
    private int columns = 80;

    public ManPageLoader() {
        sections = new ArrayList<ManSection>();
    }

    public void setFile(String filename) {
        File file = new File(filename);
        if(!file.isFile())
            throw new IllegalArgumentException(filename+" must be a file.");
        else
            this.file = file;
    }

    public void setFile(File file) {
        if(!file.isFile())
            throw new IllegalArgumentException(file+" must be a file.");
        else
            this.file = file;
    }

    public String getResourceName() {
        return file.getName();
    }

    @Override
    public List<String> loadPage(int columns) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        try {
            String line = br.readLine();
            boolean foundEmptyLine = true;
            List<String> section = new ArrayList<String>();
            while (line != null) {
                if(line.trim().isEmpty() && !foundEmptyLine) {
                    foundEmptyLine = true;
                    section.add(line);
                }
                else if(line.isEmpty() && foundEmptyLine) {
                    ManSection manSection = new ManSection().parseSection(section, 80);
                    if(manSection.getType() != SectionType.IGNORED)
                        sections.add(manSection);
                    foundEmptyLine = false;
                    section.clear();
                }
                else {
                    if(foundEmptyLine)
                        foundEmptyLine = false;
                    section.add(line);
                }

                line = br.readLine();
            }
            if(!section.isEmpty()) {
                ManSection manSection = new ManSection().parseSection(section, 80);
                if(manSection.getType() != SectionType.IGNORED)
                    sections.add(manSection);
            }

            return getAsList();
        }
        finally {
            br.close();
        }
    }

    public List<ManSection> getSections() {
        return sections;
    }

    public List<String> getAsList() {
        List<String> out = new ArrayList<String>();
        for(ManSection section : sections)
            out.addAll(section.getAsList());

        return out;
    }

    public String print() {
        StringBuilder builder = new StringBuilder();
        for(ManSection section : sections) {
            builder.append(section.printToTerminal()).append(Config.getLineSeparator());
        }

        return builder.toString();
    }
}