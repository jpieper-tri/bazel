// Copyright 2016 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.worker;

import com.google.common.base.Preconditions;
import com.google.devtools.build.lib.vfs.FileSystemUtils;
import com.google.devtools.build.lib.vfs.Path;
import com.google.devtools.build.lib.vfs.PathFragment;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

/** A {@link Worker} that runs inside a sandboxed execution root. */
final class SandboxedWorker extends Worker {
  private final Path workDir;
  private WorkerExecRoot workerExecRoot;

  SandboxedWorker(WorkerKey workerKey, int workerId, Path workDir, Path logFile) {
    super(workerKey, workerId, workDir, logFile);
    this.workDir = workDir;
  }

  @Override
  void destroy() throws IOException {
    super.destroy();
    FileSystemUtils.deleteTree(workDir);
  }

  @Override
  public void prepareExecution(
      Map<PathFragment, Path> inputFiles,
      Set<PathFragment> outputFiles,
      Set<PathFragment> workerFiles)
      throws IOException {
    Preconditions.checkState(workerExecRoot == null);
    this.workerExecRoot = new WorkerExecRoot(workDir, inputFiles, outputFiles, workerFiles);
    workerExecRoot.createFileSystem();

    super.prepareExecution(inputFiles, outputFiles, workerFiles);
  }

  @Override
  public void finishExecution(Path execRoot) throws IOException {
    super.finishExecution(execRoot);

    workerExecRoot.copyOutputs(execRoot);
    workerExecRoot = null;
  }
}
