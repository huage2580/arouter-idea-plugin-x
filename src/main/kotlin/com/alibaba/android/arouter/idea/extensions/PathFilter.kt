package com.alibaba.android.arouter.idea.extensions

import com.intellij.psi.PsiReferenceExpression
import com.intellij.usages.Usage
import com.intellij.usages.UsageInfo2UsageAdapter
import org.jetbrains.kotlin.psi.KtFile
import java.util.function.Predicate

class PathFilter(val path:String) :Predicate<Usage> {
    override fun test(usage: Usage): Boolean {
        val element = (usage as UsageInfo2UsageAdapter).element

//        //kotlin
//        if (element.containingFile is KtFile) {
//            return element.parent.text.contains(path)
//        }
//
//        //java
//        if (element is PsiReferenceExpression) {
//            return element.parent.text.contains(path)
//        }
        return element.parent.text.contains(path)
    }

}