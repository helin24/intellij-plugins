// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.intellij.terraform.template.editor

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import org.intellij.terraform.hil.psi.ILTemplateForBlockExpression
import org.intellij.terraform.hil.psi.ILTemplateIfBlockExpression
import org.intellij.terraform.template.psi.TftplVisitor

internal class TerraformTemplateFoldingBuilder : FoldingBuilder, DumbAware {

  override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
    val result = mutableListOf<FoldingDescriptor>()

    node.psi.accept(object : TftplVisitor() {
      override fun visitElement(element: PsiElement) {
        super.visitElement(element)
        element.acceptChildren(this)
      }

      override fun visitILTemplateForBlockExpression(forBlock: ILTemplateForBlockExpression) {
        addFoldingRegion(forBlock.node, forBlock.startOffset, forBlock.endOffset, result)
        super.visitILTemplateForBlockExpression(forBlock)
      }

      override fun visitILTemplateIfBlockExpression(ifBlock: ILTemplateIfBlockExpression) {
        val elseBranchOrNull = ifBlock.elseBranch

        val ifBranchStartOffset = ifBlock.startOffset
        val ifBranchEndOffset = elseBranchOrNull?.startOffset
                                ?: ifBlock.endIfBranch?.endOffset
                                ?: ifBlock.endOffset
        addFoldingRegion(ifBlock.node, ifBranchStartOffset, ifBranchEndOffset, result)

        if (elseBranchOrNull != null) {
          val elseBranchStartOffset = elseBranchOrNull.startOffset
          val elseBranchEndOffset = ifBlock.endIfBranch?.endOffset
                                    ?: ifBlock.endOffset
          addFoldingRegion(ifBlock.node, elseBranchStartOffset, elseBranchEndOffset, result)
        }

        super.visitILTemplateIfBlockExpression(ifBlock)
      }
    })

    return result.toTypedArray()
  }

  private fun addFoldingRegion(node: ASTNode, startOffset: Int, endOffset: Int, result: MutableList<FoldingDescriptor>) {
    if (endOffset <= startOffset) return
    val blockRange = TextRange.create(startOffset, endOffset)
    if (!blockRange.isEmpty) {
      result.add(FoldingDescriptor(
        node,
        blockRange,
        null,
        " ... ",
        false,
        emptySet()))
    }
  }

  override fun getPlaceholderText(node: ASTNode): String? = null

  override fun isCollapsedByDefault(node: ASTNode): Boolean = false
}