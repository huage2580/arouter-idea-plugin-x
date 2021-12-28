package com.alibaba.android.arouter.idea.extensions

import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtFile
import java.util.function.Predicate

class PathFilter(val path:String) :Predicate<Usage> {
    override fun test(usage: Usage): Boolean {
        val element = (usage as UsageInfo2UsageAdapter).element ?: return false

        //kotlin
        if (element.containingFile is KtFile) {
            val target = KtNavigationLineMarker.resolvePath((element.parent as KtCallExpression).valueArguments[0])
            if (target.contains(path)){
                return true
            }
        }

        //java
        if (element is PsiReferenceExpression) {
            val target = NavigationLineMarker.resolvePath((element.parent as PsiMethodCallExpressionImpl).argumentList.expressions[0])
            if (target.contains(path)){
                return true
            }
        }
        return element.parent.text.contains(path)
    }

}