package org.jetbrains.astro.service

import com.intellij.lang.javascript.service.BaseLspTypeScriptServiceTest
import com.intellij.lang.javascript.library.typings.TypeScriptExternalDefinitionsRegistry
import com.intellij.lang.typescript.compiler.languageService.TypeScriptLanguageServiceUtil
import com.intellij.lang.typescript.library.download.TypeScriptDefinitionFilesDirectory
import com.intellij.openapi.util.Disposer
import org.jetbrains.astro.service.settings.AstroServiceMode
import org.jetbrains.astro.service.settings.getAstroServiceSettings

open class AstroServiceTestBase : BaseLspTypeScriptServiceTest() {
  override fun getExtension(): String = "astro"

  override fun setUp() {
    super.setUp()

    val serviceSettings = getAstroServiceSettings(project)
    val old = serviceSettings.serviceMode
    TypeScriptLanguageServiceUtil.setUseService(true)
    TypeScriptExternalDefinitionsRegistry.testTypingsRootPath = TypeScriptDefinitionFilesDirectory.getGlobalAutoDownloadTypesDirectoryPath()

    Disposer.register(testRootDisposable) {
      serviceSettings.serviceMode = old
      TypeScriptLanguageServiceUtil.setUseService(false)
    }
    serviceSettings.serviceMode = AstroServiceMode.ENABLED

    ensureServerDownloaded(AstroLspExecutableDownloader)
  }

  protected fun assertCorrectService() = assertCorrectServiceImpl<AstroLspTypeScriptService>()

  protected fun configureDefault() = myFixture.configureByFile(getTestName(false) + "." + extension)
}