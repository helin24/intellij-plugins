/*
 * Copyright 2013-2016 Sergey Ignatov, Alexander Zolotov, Florin Patan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.intellij.terraform.config.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ExceptionUtil;
import org.intellij.terraform.config.TerraformConstants;
import org.intellij.terraform.config.TerraformFileType;
import org.intellij.terraform.hcl.HCLFileType;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("WeakerAccess")
public abstract class TFExternalToolsAction extends DumbAwareAction {
  private static final Logger LOG = Logger.getInstance(TFExternalToolsAction.class);

  private static void error(@NotNull @Nls String title, @NotNull Project project, @Nullable Exception ex) {
    String message = ex == null ? "" : ExceptionUtil.getUserStackTrace(ex, LOG);
    TerraformConstants.EXECUTION_NOTIFICATION_GROUP.createNotification(title, message, NotificationType.ERROR).notify(project);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || file == null || !isAvailableOnFile(file, true, true)) {
      e.getPresentation().setEnabled(false);
      return;
    }
    e.getPresentation().setEnabled(true);
  }

  static boolean isAvailableOnFile(@NotNull final VirtualFile file, boolean checkDirChildren, boolean onlyTerraformFileType) {
    if (!file.isInLocalFileSystem()) return false;
    if (file.isDirectory()) {
      if (!checkDirChildren) return false;
      //noinspection UnsafeVfsRecursion
      VirtualFile[] children = file.getChildren();
      if (children != null) {
        for (VirtualFile child : children) {
          if (isAvailableOnFile(child, false, onlyTerraformFileType)) return true;
        }
      }
      return false;
    }

    return onlyTerraformFileType
           ? FileTypeRegistry.getInstance().isFileOfType(file, TerraformFileType.INSTANCE)
           : FileTypeRegistry.getInstance().isFileOfType(file, HCLFileType.INSTANCE) ||
             FileTypeRegistry.getInstance().isFileOfType(file, TerraformFileType.INSTANCE);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    VirtualFile file = e.getRequiredData(CommonDataKeys.VIRTUAL_FILE);
    assert project != null;
    String title = StringUtil.notNullize(e.getPresentation().getText());

    Module module = ModuleUtilCore.findModuleForFile(file, project);
    try {
      invoke(project, module, title, file);
    } catch (ExecutionException ex) {
      error(title, project, ex);
      LOG.error(ex);
    }
  }

  abstract void invoke(@NotNull Project project,
                       @Nullable Module module,
                       @NotNull @Nls String title,
                       @NotNull VirtualFile virtualFile) throws ExecutionException;

}
