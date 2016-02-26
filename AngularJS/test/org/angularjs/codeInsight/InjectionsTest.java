package org.angularjs.codeInsight;

import com.intellij.lang.css.CSSLanguage;
import com.intellij.lang.html.HTMLLanguage;
import com.intellij.lang.javascript.JSTestUtils;
import com.intellij.lang.javascript.dialects.JSLanguageLevel;
import com.intellij.lang.javascript.psi.JSDefinitionExpression;
import com.intellij.lang.javascript.psi.JSNamedElement;
import com.intellij.lang.javascript.psi.JSVariable;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import com.intellij.lang.javascript.psi.resolve.ImplicitJSVariableImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.testFramework.fixtures.LightPlatformCodeInsightFixtureTestCase;
import com.intellij.util.ThrowableRunnable;
import com.sixrr.inspectjs.confusing.CommaExpressionJSInspection;
import com.sixrr.inspectjs.style.UnterminatedStatementJSInspection;
import com.sixrr.inspectjs.validity.BadExpressionStatementJSInspection;
import org.angularjs.AngularTestUtil;
import org.angularjs.lang.AngularJSLanguage;

import java.util.List;

/**
 * @author Dennis.Ushakov
 */
public class InjectionsTest extends LightPlatformCodeInsightFixtureTestCase {
  @Override
  protected String getTestDataPath() {
    return AngularTestUtil.getBaseTestDataPath(getClass()) + "injections";
  }

  @Override
  protected boolean isWriteActionRequired() {
    return getTestName(true).contains("Completion");
  }

  public void testNgInitCompletion() {
    myFixture.testCompletion("ngInit.html", "ngInit.after.html", "angular.js");
  }

  public void testNgInitResolve() {
    myFixture.configureByFiles("ngInit.resolve.html", "angular.js");
    checkVariableResolve("fri<caret>ends", "friends", JSDefinitionExpression.class);
  }

  public void testNgRepeatImplicitCompletion() {
    myFixture.configureByFiles("ngRepeatImplicit.html", "angular.js");
    myFixture.testCompletionVariants("ngRepeatImplicit.html", "$index", "$first", "$middle", "$last", "$even", "$odd");
  }

  public void testNgRepeatAfterDotCompletion() {
    myFixture.configureByFiles("ngRepeatImplicitAfterDot.html", "angular.js");
    final List<String> variants = myFixture.getCompletionVariants("ngRepeatImplicitAfterDot.html");
    assertDoesntContain(variants, "person", "$index", "$first", "$middle", "$last", "$even", "$odd");
  }

  public void testNgRepeatImplicitResolve() {
    myFixture.configureByFiles("ngRepeatImplicitType.html", "angular.js");
    final PsiElement resolve = checkVariableResolve("ind<caret>ex", "$index", ImplicitJSVariableImpl.class);
    assertEquals("Number", ((JSVariable)resolve).getTypeString());
  }

  public void testNgRepeatExplicitCompletion() {
    myFixture.testCompletion("ngRepeatExplicit.html", "ngRepeatExplicit.after.html", "angular.js");
  }

  public void testInternalDefinitionCompletion() {
    myFixture.testCompletion("internalDefinition.html", "internalDefinition.after.html", "angular.js");
  }

  public void testNgRepeatExplicitCompletionInScript() {
    myFixture.testCompletion("ngRepeatExplicitInScript.html", "ngRepeatExplicitInScript.after.html", "angular.js");
  }

  public void testNgRepeatExplicitCompletionSameTag() {
    myFixture.testCompletion("ngRepeatExplicitSameTag.html", "ngRepeatExplicitSameTag.after.html", "angular.js");
  }

  public void testNgRepeatExplicitResolve() {
    myFixture.configureByFiles("ngRepeatExplicit.resolve.html", "angular.js");
    checkVariableResolve("per<caret>son", "person", JSDefinitionExpression.class);
  }

  public void testInternalDefinitionResolve() {
    myFixture.configureByFiles("internalDefinition.after.html", "angular.js");
    checkVariableResolve("ord<caret>ered[", "ordered", JSDefinitionExpression.class);
  }

  public void testNgRepeatExplicitResolveInScript() {
    myFixture.configureByFiles("ngRepeatExplicitInScript.resolve.html", "angular.js");
    checkVariableResolve("per<caret>son", "person", JSDefinitionExpression.class);
  }

  public void testNgRepeatExplicitResolveSameTag() {
    myFixture.configureByFiles("ngRepeatExplicitSameTag.resolve.html", "angular.js");
    checkVariableResolve("per<caret>son", "person", JSDefinitionExpression.class);
  }

  public void testNgRepeatExplicitKeyCompletion() {
    myFixture.testCompletion("ngRepeatExplicitHashKey.html", "ngRepeatExplicitHashKey.after.html", "angular.js");
  }

  public void testNgRepeatExplicitKeyResolve() {
    myFixture.configureByFiles("ngRepeatExplicitHashKey.resolve.html", "angular.js");
    checkVariableResolve("ke<caret>y", "key", JSDefinitionExpression.class);
  }

  public void testNgRepeatExplicitValueCompletion() {
    myFixture.testCompletion("ngRepeatExplicitHashValue.html", "ngRepeatExplicitHashValue.after.html", "angular.js");
  }

  public void testNgRepeatExplicitValueResolve() {
    myFixture.configureByFiles("ngRepeatExplicitHashValue.resolve.html", "angular.js");
    checkVariableResolve("val<caret>ue", "value", JSDefinitionExpression.class);
  }

  public void testNgControllerAliasCompletion() {
    myFixture.testCompletionTyping("ngControllerAlias.html", "\n", "ngControllerAlias.after.html", "angular.js", "custom.js");
  }

  public void testNgControllerAliasHighlighting() {
    myFixture.configureByFiles("ngControllerAlias.highlight.html", "angular.js", "custom.js");
    myFixture.checkHighlighting();
  }

  public void testNgControllerAliasResolve() {
    myFixture.configureByFiles("ngControllerAlias.resolve.html", "angular.js", "custom.js");
    checkVariableResolve("ap<caret>p", "app", JSDefinitionExpression.class);
  }
  
  public void testEmmetBeforeInjection() {
    myFixture.configureByFiles("ngController.emmet.html", "angular.js", "custom.js");
    myFixture.type('\t');
    myFixture.checkResultByFile("ngController.emmet.after.html");
  }

  public void testComment() {
    myFixture.configureByFiles("comment.html", "angular.js");
  }

  public void testCustomDelimiters() {
    myFixture.configureByFiles("customDelimiters.html", "angular.js", "customDelimiters.js");
    assertEquals(AngularJSLanguage.INSTANCE, myFixture.getFile().getLanguage());
  }

  public void testCustomDelimitersInline() {
    myFixture.configureByFiles("customDelimitersInline.html", "angular.js", "customDelimitersInline.js");
    assertEquals(HTMLLanguage.INSTANCE, myFixture.getFile().getLanguage());
  }

  public void testCustomDelimitersDefaultIgnored() {
    myFixture.configureByFiles("customDelimitersDefaultIgnored.html", "angular.js", "customDelimitersDefaultIgnored.js");
    assertNotSame(AngularJSLanguage.INSTANCE, myFixture.getFile().getLanguage());
  }

  public void testDefaultDelimitersInJSX() throws Exception {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.JSX, getProject(), new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myFixture.configureByFiles("defaultDelimiters.jsx", "angular.js");
        assertNotSame(AngularJSLanguage.INSTANCE, myFixture.getFile().getLanguage());
      }
    });
  }

  public void testCustomDelimitersSameStartEnd() {
    myFixture.configureByFiles("customDelimitersSameStartEnd.html", "angular.js", "customDelimitersSameStartEnd.js");
    assertEquals(AngularJSLanguage.INSTANCE, myFixture.getFile().getLanguage());
  }

  public void testNoQuotes() {
    myFixture.configureByFiles("noQuotes.html", "angular.js");
  }

  public void testBadExpression() {
    myFixture.enableInspections(BadExpressionStatementJSInspection.class);
    myFixture.configureByFiles("badExpression.html", "angular.js", "custom.js");
    myFixture.checkHighlighting();
  }

  public void testCommaExpression() {
    myFixture.enableInspections(CommaExpressionJSInspection.class);
    myFixture.configureByFiles("commaExpression.html", "angular.js", "custom.js");
    myFixture.checkHighlighting();
  }

  public void testEventHandler2Resolve() throws Exception {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myFixture.configureByFiles("event.html", "angular2.js", "event.ts");
        checkVariableResolve("callAnonymous<caret>Api()", "callAnonymousApi", TypeScriptFunction.class);
      }
    });
  }

  public void testBinding2Resolve() throws Exception {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myFixture.configureByFiles("binding.html", "angular2.js", "event.ts");
        checkVariableResolve("callAnonymous<caret>Api()", "callAnonymousApi", TypeScriptFunction.class);
      }
    });
  }

  private PsiElement checkVariableResolve(final String signature, final String varName, final Class<? extends JSNamedElement> varClass) {
    int offsetBySignature = AngularTestUtil.findOffsetBySignature(signature, myFixture.getFile());
    PsiReference ref = myFixture.getFile().findReferenceAt(offsetBySignature);
    assertNotNull(ref);
    PsiElement resolve = ref.resolve();
    assertInstanceOf(resolve, varClass);
    assertEquals(varName, varClass.cast(resolve).getName());
    return resolve;
  }

  public void testMessageFormat() {
    System.setProperty("angular.js.parse.message.format", "true");
    try {
      myFixture.configureByFiles("messageFormat.html", "messageFormatController.js", "angular.js");
      myFixture.checkHighlighting();
    } finally {
      System.clearProperty("angular.js.parse.message.format");
    }
  }

  public void testStyles2() throws Exception {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myFixture.configureByFiles("custom.ts", "angular2.js");
        final int offset = AngularTestUtil.findOffsetBySignature("Helvetica <caret>Neue", myFixture.getFile());
        final PsiElement element = InjectedLanguageUtil.findElementAtNoCommit(myFixture.getFile(), offset);
        assertEquals(CSSLanguage.INSTANCE, element.getLanguage());
      }
    });
  }
  public void testUnterminated() throws Exception {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<Exception>() {
      @Override
      public void run() throws Exception {
        myFixture.enableInspections(UnterminatedStatementJSInspection.class);
        myFixture.configureByFiles("unterminated.ts", "angular2.js");
        myFixture.checkHighlighting();
      }
    });
  }

  public void testNgForExternalCompletion() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<RuntimeException>() {
      @Override
      public void run() throws RuntimeException {
        myFixture.testCompletion("ngFor.html", "ngFor.after.html", "angular2.js");
      }
    });
  }

  public void testNgForExternalResolve() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<RuntimeException>() {
      @Override
      public void run() throws RuntimeException {
        myFixture.configureByFiles("ngFor.after.html", "angular2.js");
        checkVariableResolve("myTo<caret>do", "myTodo", JSDefinitionExpression.class);
      }
    });
  }

  public void testNgForInlineCompletion() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<RuntimeException>() {
      @Override
      public void run() throws RuntimeException {
        myFixture.testCompletion("ngFor.ts", "ngFor.after.ts", "angular2.js");
      }
    });
  }

  public void testNgForInlineResolve() {
    JSTestUtils.testWithinLanguageLevel(JSLanguageLevel.ES6, getProject(), new ThrowableRunnable<RuntimeException>() {
      @Override
      public void run() throws RuntimeException {
        myFixture.configureByFiles("ngFor.after.ts", "angular2.js");
        checkVariableResolve("\"myTo<caret>do\"", "myTodo", JSDefinitionExpression.class);
      }
    });
  }
}
