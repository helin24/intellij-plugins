package com.intellij.deno

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.deno.roots.createDenoEntity
import com.intellij.deno.roots.removeDenoEntity
import com.intellij.deno.service.DenoLspSupportProvider
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.javascript.TypeScriptFileType
import com.intellij.lang.javascript.TypeScriptJSXFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.*
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.AdditionalLibraryRootsListener
import com.intellij.openapi.roots.AdditionalLibraryRootsProvider
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.ThrowableRunnable

enum class UseDeno {
  CONFIGURE_AUTOMATICALLY,
  ENABLE,
  DISABLE,
}

const val DENO_CONFIG_JSON_NAME = "deno.json"
const val DENO_CONFIG_JSONC_NAME = "deno.jsonc"
private const val useLibraryKey = "deno.use.library"

class DenoState : Cloneable {

  @Deprecated("old state, do not use, keep for backward-compatibility")
  var useDeno = false

  var useDenoValue: UseDeno = UseDeno.CONFIGURE_AUTOMATICALLY
  var denoPath = ""
  var denoCache = ""
  var denoInit = getDefaultInitTemplate()

  public override fun clone(): DenoState {
    val nextState = DenoState()
    @Suppress("DEPRECATION")
    nextState.useDeno = this.useDeno
    nextState.useDenoValue = this.useDenoValue
    nextState.denoPath = this.denoPath
    nextState.denoCache = this.denoCache
    nextState.denoInit = this.denoInit
    return nextState
  }
}

@Service(Service.Level.PROJECT)
@State(name = "DenoSettings", storages = [Storage("deno.xml")])
class DenoSettings(private val project: Project) : PersistentStateComponent<DenoState> {
  companion object {
    fun getService(project: Project): DenoSettings = project.service<DenoSettings>()
  }

  private var state = DenoState()

  override fun getState(): DenoState {
    return state
  }

  override fun loadState(state: DenoState) {
    var newState = state
    @Suppress("DEPRECATION")
    if (state.useDeno) {
      newState = state.clone()
      newState.useDeno = false
      newState.useDenoValue = UseDeno.ENABLE
    }

    this.state = newState
  }

  fun setUseDeno(useDeno: UseDeno) {
    this.state.useDenoValue = useDeno
  }

  fun isConfigureDenoAutomatically(): Boolean {
    return this.state.useDenoValue == UseDeno.CONFIGURE_AUTOMATICALLY
  }

  fun isEnableDeno(): Boolean {
    return this.state.useDenoValue == UseDeno.ENABLE
  }

  fun isDisableDeno(): Boolean {
    return this.state.useDenoValue == UseDeno.DISABLE
  }

  /**
   * @return information enabling / disabling the deno integration.
   * In most of the case {@link #isDenoEnableForContext} should be used instead
   */
  fun isUseDeno(): Boolean {
    return this.state.useDenoValue == UseDeno.CONFIGURE_AUTOMATICALLY || this.state.useDenoValue == UseDeno.ENABLE
  }

  fun getUseDeno(): UseDeno {
    return this.state.useDenoValue
  }

  fun getDenoPath(): String {
    val denoPath = this.state.denoPath
    if (denoPath.isEmpty()) {
      return DenoUtil.getDefaultDenoExecutable() ?: ""
    }
    return denoPath
  }

  fun setDenoPath(path: String) {
    val defaultPath = DenoUtil.getDefaultDenoExecutable() ?: ""
    this.state.denoPath = if (defaultPath == path) "" else path
  }

  fun getDenoCache(): String {
    val denoCache = this.state.denoCache
    if (denoCache.isEmpty()) {
      return DenoUtil.getDenoCache()
    }
    return denoCache
  }

  fun getDenoCacheDeps(): String {
    return getDenoCache() + "/deps"
  }

  fun getDenoNpm(): String {
    return getDenoCache() + "/npm"
  }

  fun setDenoCache(path: String) {
    this.state.denoCache = if (DenoUtil.getDenoCache() == path) "" else path
  }

  fun getDenoInit(): String {
    return state.denoInit
  }

  fun setDenoInit(denoInit: String) {
    state.denoInit = denoInit
  }

  fun setUseDenoAndReload(useDeno: UseDeno) {
    val libraryProvider = AdditionalLibraryRootsProvider.EP_NAME.findExtensionOrFail(DenoLibraryProvider::class.java)
    val oldRoots = libraryProvider.getRootsToWatch(project)
    if (useDeno != getUseDeno()) {
      setUseDeno(useDeno)

      if (!project.isDefault) {
        val lspServerManager = LspServerManager.getInstance(project)
        if (useDeno == UseDeno.ENABLE || useDeno == UseDeno.CONFIGURE_AUTOMATICALLY) {
          lspServerManager.startServersIfNeeded(DenoLspSupportProvider::class.java)
          createDenoEntity(project)
        }
        else {
          lspServerManager.stopServers(DenoLspSupportProvider::class.java)
          removeDenoEntity(project)
        }
      }
    }

    WriteAction.run(
      ThrowableRunnable<ConfigurationException> {
        val newRoots = libraryProvider.getRootsToWatch(project)
        AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(project, null, oldRoots, newRoots, "Deno")

        DaemonCodeAnalyzer.getInstance(project).restart()
      })
  }

  fun updateLibraries() {
    val libraryProvider = AdditionalLibraryRootsProvider.EP_NAME.findExtensionOrFail(DenoLibraryProvider::class.java)
    val oldRoots = libraryProvider.getRootsToWatch(project)
    WriteAction.run(
      ThrowableRunnable<ConfigurationException> {
        val newRoots = libraryProvider.getRootsToWatch(project)
        AdditionalLibraryRootsListener.fireAdditionalLibraryChanged(project, null, oldRoots, newRoots, "Deno")

        DaemonCodeAnalyzer.getInstance(project).restart()
      })
  }
}

fun isDenoEnableForContext(psiElement: PsiElement): Boolean {
  val virtualFile = PsiUtilCore.getVirtualFile(psiElement.containingFile.originalFile) ?: return false

  return isDenoFileTypeAcceptable(virtualFile) && isDenoEnableForContextDirectory(psiElement.project, virtualFile)
}

fun isDenoEnableForContextDirectory(project: Project, virtualFile: VirtualFile?): Boolean {
  if (virtualFile == null || !virtualFile.isInLocalFileSystem) return false

  val denoSettings = DenoSettings.getService(project)

  if (denoSettings.isConfigureDenoAutomatically() || denoSettings.isEnableDeno()) {
    if (virtualFile.path.startsWith(denoSettings.getDenoCache())) {
      return true
    }
  }

  return denoSettings.isConfigureDenoAutomatically() && isDenoConfigAvailable(project, virtualFile) ||
         denoSettings.isEnableDeno()
}

fun getDefaultInitTemplate() = """
      {
        "enable": true,
        "lint": true,
        "unstable": true,
        "importMap": "import_map.json",
        "config": "deno.json"
      }
    """.trimIndent()

fun findDenoConfig(project: Project, file: VirtualFile?): VirtualFile? {
  return file?.let {
    val root = project.guessProjectDir()
    var current = if (file.isDirectory) file else file.parent
    while (current != null) {
      val config = current.findChild(DENO_CONFIG_JSON_NAME) ?: current.findChild(DENO_CONFIG_JSONC_NAME)
      if (config != null && config.isValid && !config.isDirectory) return@let config
      if (current == root) break

      current = current.parent
    }
    return@let null
  }
}

private fun isDenoConfigAvailable(project: Project, file: VirtualFile?): Boolean {
  return findDenoConfig(project, file) != null
}

fun useDenoLibrary(project: Project): Boolean {
  return DenoSettings.getService(project).isEnableDeno() ||
         PropertiesComponent.getInstance(project).getBoolean(useLibraryKey, false)
}

fun setUseDenoLibrary(project: Project) {
  PropertiesComponent.getInstance(project).setValue(useLibraryKey, true)
  ApplicationManager.getApplication().invokeLater { DenoSettings.getService(project).updateLibraries() }
}

fun isDenoFileTypeAcceptable(file: VirtualFile) =
  file.fileType in arrayOf(TypeScriptFileType.INSTANCE, TypeScriptJSXFileType.INSTANCE)

