/*
 * This file is part of JavaDowngrader - https://github.com/RaphiMC/JavaDowngrader
 * Copyright (C) 2023-2024 RK_01/RaphiMC and contributors
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.raphimc.javadowngrader.standalone.progress;

import me.tongfei.progressbar.ConsoleProgressBarConsumer;
import me.tongfei.progressbar.ProgressBarBuilder;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.*;

class ThreadedLineProgressBar extends SimpleProgressBar {
    private final Map<Thread, String> tasks = new LinkedHashMap<>();

    public ThreadedLineProgressBar(ProgressBarBuilder builder) {
        this(builder, new TheConsumer(new PrintStream(new FileOutputStream(FileDescriptor.err))));
    }

    public ThreadedLineProgressBar(ProgressBarBuilder builder, TheConsumer consumer) {
        super(builder.setConsumer(consumer).build());
        consumer.bar = this;
    }

    @Override
    public void step() {
        setThreadTask("IDLE");
        super.step();
    }

    @Override
    public void setThreadTask(String task) {
        tasks.put(Thread.currentThread(), task);
    }

    private static class TheConsumer extends ConsoleProgressBarConsumer {
        private final PrintStream out;
        private ThreadedLineProgressBar bar;
        private List<String> oldTasks;

        public TheConsumer(PrintStream out) {
            super(out);
            this.out = out;
        }

        @Override
        public void accept(String str) {
            if (oldTasks != null) {
                out.print(TerminalUtils.moveCursorUp(oldTasks.size()));
            } else {
                oldTasks = Collections.emptyList();
            }
            super.accept(str);
            if (bar == null) return;
            final List<String> tasks = new ArrayList<>(bar.tasks.values());
            int i = 0;
            for (final String task : tasks) {
                final String oldTask = i < oldTasks.size() ? oldTasks.get(i) : "";
                final StringBuilder display = new StringBuilder("\n> ").append(task);
                if (oldTask.length() > task.length()) {
                    for (int j = 0, l = oldTask.length() - task.length(); j < l; j++) {
                        display.append(' ');
                    }
                }
                out.print(display);
                i++;
            }
            oldTasks = tasks;
            out.flush();
        }

        @Override
        public void close() {
            if (oldTasks != null) {
                final StringBuilder clear = new StringBuilder();
                for (int i = oldTasks.size() - 1; i >= 0; i--) {
                    final int taskLen = oldTasks.get(i).length() + 2;
                    clear.append('\r');
                    for (int j = 0; j < taskLen + 2; j++) {
                        clear.append(' ');
                    }
                    clear.append(TerminalUtils.moveCursorUp(1));
                }
                out.print(clear);
            }
            super.close();
        }
    }
}
