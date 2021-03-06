/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Eclipse Public License version 1.0, available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.jboss.aesh.extensions.matrix;

import org.jboss.aesh.terminal.CharacterType;
import org.jboss.aesh.terminal.Color;
import org.jboss.aesh.terminal.Shell;
import org.jboss.aesh.terminal.TerminalColor;
import org.jboss.aesh.terminal.TerminalTextStyle;
import org.jboss.aesh.util.ANSI;
import org.jboss.aesh.util.LoggerUtil;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Math.random;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class MatrixRunner implements Runnable {

    private static Logger logger = LoggerUtil.getLogger("MatrixRunner.class");

    private final Shell shell;
    private final MatrixPoint[][] matrix;
    private final int[] delay;
    private final int columns;
    private final int rows;
    private boolean running = true;
    private boolean async = true;
    private int speed;

    private static final TerminalTextStyle TEXT_BOLD = new TerminalTextStyle(CharacterType.BOLD);
    private static final TerminalTextStyle TEXT_FAINT = new TerminalTextStyle(CharacterType.BOLD);
    private static final TerminalColor GREEN_COLOR = new TerminalColor(Color.GREEN, Color.DEFAULT);
    private static final TerminalColor DEFAULT_COLOR = new TerminalColor(Color.DEFAULT, Color.DEFAULT);

    public MatrixRunner(Shell shell, List<String> knockStrings, InputStream inputText,
                        int speed, boolean async) {
        this.shell = shell;
        this.async = async;
        this.speed = speed;
        columns = shell.getSize().getWidth();
        rows = shell.getSize().getHeight();
        matrix = new MatrixPoint[rows][columns];
        delay = new int[columns];

        setupMatrix(knockStrings, inputText);
    }

    private void setupMatrix(List<String> knockStrings, InputStream inputStream) {
        for(int i=0; i < rows; i++) {
            for(int j=0; j < columns; j++) {
                matrix[i][j] = new MatrixPoint(rows, columns, i+1,j+1);
                if(i == 0) {
                    delay[j] = (int) (random() * 3) +2;
                }
            }
        }
        shell.out().print(GREEN_COLOR.fullString());
        if(knockStrings != null)
            knockKnock(knockStrings);
        if(inputStream != null)
            readFile(inputStream);
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        int counter = 1;
        int sleepTime;
        long startTime;
        try {
            while(running) {
                startTime = System.currentTimeMillis();
                counter++;
                for(int i=0; i < rows; i++) {
                    for(int j=0; j < columns; j +=2) {
                        if(counter > delay[j] || !async) {
                        if(i == 0) {
                            if(!matrix[i][j].isPartOfTextOrSpace()) {
                                matrix[i][j].newCycle();
                                matrix[i][j].getChanges(shell);
                            }
                            else {
                                matrix[i][j].nextCycle();
                                matrix[i][j].getChanges(shell);
                            }
                        }
                        else {
                            if( !matrix[i][j].isPartOfTextOrSpace()) {
                                if(matrix[i-1][j].isNextUp()) {
                                    matrix[i][j].newCycle( matrix[i-1][j].getPosition()-1, matrix[i-1][j].getLength(), true);
                                    matrix[i][j].getChanges(shell);
                                }
                            }
                            else if(matrix[i][j].isPartOfTextOrSpace()) {
                                matrix[i][j].nextCycle();
                                matrix[i][j].getChanges(shell);
                            }

                        }
                        }
                    }
                }

                shell.out().flush();

                sleepTime = (speed * 8) - (int) (System.currentTimeMillis()-startTime);
                if(sleepTime > 0)
                    Thread.sleep(sleepTime);


                if(counter > 4)
                    counter = 1;
            }

        }
        catch(IOException | InterruptedException ioe) {
            logger.warning(ioe.getMessage());
        }
    }

    private void knockKnock(List<String> knockStrings) {
        try {
            shell.out().print(ANSI.showCursor());
            for(String knock : knockStrings) {
                showKnock(knock);
                Thread.sleep(2000);
                shell.clear();
            }
            shell.out().print(ANSI.hideCursor());
        }
        catch (InterruptedException | IOException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }
    }

    private void showKnock(String knock) throws InterruptedException {
        shell.out().print( ANSI.getStart()+ 1 +";"+ 1 +"H"); // moveCursor(rows, columns);
        for(char c : knock.toCharArray()) {
            shell.out().print(c);
            shell.out().flush();
            Thread.sleep(40);
        }

    }

    private void readFile(InputStream stream) {
        List<String> lines = new ArrayList<>();
        try {
            InputStreamReader inputReader = new InputStreamReader(stream);
            BufferedReader br = new BufferedReader(inputReader);

            String line = br.readLine();
            while(line != null) {
                lines.add(line);
                line = br.readLine();
            }
            br.close();
        }
        catch (IOException e) {
            e.printStackTrace();
            logger.warning(e.getMessage());
        }

        int height = shell.getSize().getHeight();
        shell.out().print( ANSI.getStart()+ height +";"+ 1 +"H"); //   moveCursor(rows, columns);

        if(lines.size() > 0) {
            int counter = 0;
            for(int i = lines.size()-1; i > -1; i--) {
                int columnCounter = 0;
                for(char c : lines.get(i).toCharArray()) {
                    shell.out().print(c);
                    if(c != ' ')
                        matrix[height-counter-1][columnCounter].setDefaultCharacter(c);

                    columnCounter++;
                    try {
                        Thread.sleep(10);
                        shell.out().flush();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                counter++;
                shell.out().print( ANSI.getStart()+ (height - counter)+";"+ 1 +"H"); //   moveCursor(rows, columns);
            }

            shell.out().flush();
        }

    }

    public void stop() {
        running = false;
        shell.out().print(DEFAULT_COLOR.fullString());
    }

    public void asynch() {
        async = !async;
    }

    public void speed(int s) {
        if(s > 0 && s < 9)
            speed = s;
    }

}
