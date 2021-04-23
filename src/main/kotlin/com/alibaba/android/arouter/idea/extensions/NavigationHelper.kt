package com.alibaba.android.arouter.idea.extensions

import com.intellij.ide.util.PsiClassListCellRenderer
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMember
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.intellij.ui.awt.RelativePoint
import java.awt.event.MouseEvent
import javax.swing.ListSelectionModel


/**
 * 跳转到目标类，多个目标列表选择
 */
object NavigationHelper {

    const val ROUTE_ANNOTATION_NAME = "com.alibaba.android.arouter.facade.annotation.Route"
    // I'm 100% sure this point can not made memory leak.
    private var routeAnnotationWrapper: PsiClass? = null

    fun findTargetAndNavigate(psiElement: PsiElement, targetPath: String, e: MouseEvent?):Boolean{
        val fullScope = GlobalSearchScope.allScope(psiElement.project)
        val routeAnnotationWrapper = AnnotatedMembersSearch.search(getAnnotationWrapper(psiElement, fullScope)
                ?: return false, fullScope).findAll()

        val targetList = routeAnnotationWrapper.filter {list->
            list.modifierList?.annotations?.map { it.findAttributeValue("path")?.text?.replace("\"", "") }?.contains(targetPath)
                    ?: false
        }

        when {
            targetList.isEmpty() -> {
                return false
            }
            targetList.size == 1 -> {
                gotoTargetClass(targetList[0])
            }
            else -> {
                showSelectClassDialog(targetList,e)
            }
        }
        return true
    }

    private fun showSelectClassDialog(targetList: List<PsiMember>, e: MouseEvent?){
        val renderer = PsiClassListCellRenderer()
        JBPopupFactory.getInstance()
                .createPopupChooserBuilder<Any>(targetList)
                .setTitle("Choose target")
                .setMovable(false)
                .setResizable(false)
                .setRequestFocus(true)
                .setCancelOnWindowDeactivation(false)
                .setRenderer(renderer)
                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
                .setItemChosenCallback{
                    gotoTargetClass(it as PsiClass)
                }
                .createPopup()
                .show(RelativePoint(e!!))

    }

    private fun gotoTargetClass(target:PsiElement){
        NavigationItem::class.java.cast(target).navigate(true)
    }

    private fun getAnnotationWrapper(psiElement: PsiElement?, scope: GlobalSearchScope): PsiClass? {
        if (null == routeAnnotationWrapper) {
            routeAnnotationWrapper = JavaPsiFacade.getInstance(psiElement?.project).findClass(ROUTE_ANNOTATION_NAME, scope)
        }

        return routeAnnotationWrapper
    }
}