package com.alibaba.android.arouter.idea.extensions

import com.intellij.ide.util.PsiClassListCellRenderer
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiAnnotationImpl
import com.intellij.psi.impl.source.tree.java.PsiAnnotationParamListImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedMembersSearch
import com.intellij.ui.awt.RelativePoint
import org.jetbrains.kotlin.idea.inspections.findExistingEditor
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgumentList
import usages.ShowUsagesAction
import java.awt.event.MouseEvent
import javax.swing.ListSelectionModel


/**
 * 跳转到目标类，多个目标列表选择
 */
object NavigationHelper {

    const val ROUTE_ANNOTATION_NAME = "com.alibaba.android.arouter.facade.annotation.Route"
    const val ROUTER_FULL_NAME = "com.alibaba.android.arouter.launcher.ARouter"
    // I'm 100% sure this point can not made memory leak.
    private var routeAnnotationWrapper: PsiClass? = null
    private var routeMethodWrapper: PsiMethod? = null


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

    fun findUsagesMethod(psiElement: PsiElement,e: MouseEvent?):Boolean{
        var path:String = ""
        if (psiElement is KtAnnotationEntry){//kotlin
            path = (psiElement.children[1] as KtValueArgumentList).arguments[0].getArgumentExpression()?.text?:return false
        }
        if (psiElement is PsiAnnotationImpl){//java
            path = (psiElement.children[2] as PsiAnnotationParamListImpl).attributes[0].detachedValue?.text?.replace("\"", "")?:return false
        }

        val fullScope = GlobalSearchScope.allScope(psiElement.project)
        val method = getMethodWrapper(psiElement,fullScope) ?: return false
//        val methodUsageList = MethodReferencesSearch.search(method,fullScope,true).findAll()
//        val findList = methodUsageList.map { it.element }.filter {
//            it.parent.text.contains(path)
//        }.toList()
//        showUsagesDialog(findList,e)

        ShowUsagesAction(PathFilter(path)).startFindUsages(method, RelativePoint(e!!),psiElement.findExistingEditor(),100)
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


//    private fun showUsagesDialog(targetList: List<PsiElement>, e: MouseEvent?){
//        ShowUsagesAction()
//        JBPopupFactory.getInstance()
//                .createPopupChooserBuilder<Any>(targetList)
//                .setTitle("Usages")
//                .setMovable(false)
//                .setResizable(false)
//                .setRequestFocus(true)
//                .setCancelOnWindowDeactivation(false)
//                .setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
//                .setItemChosenCallback{
//                    gotoUsageMethod(it)
//                }
//                .createPopup()
//                .show(RelativePoint(e!!))
//
//    }
//
//    private fun gotoUsageMethod(it: Any) {
//        //JAVA的应用
//        if (it is PsiReferenceExpressionImpl){
//            it.navigate(true)
//        }
//        //Kotlin
//        if (it is KtNameReferenceExpression){
//            it.navigate(true)
//        }
//    }

    private fun gotoTargetClass(target:PsiElement){
        NavigationItem::class.java.cast(target).navigate(true)
    }

    private fun getAnnotationWrapper(psiElement: PsiElement?, scope: GlobalSearchScope): PsiClass? {
        if (null == routeAnnotationWrapper) {
            routeAnnotationWrapper = JavaPsiFacade.getInstance(psiElement?.project).findClass(ROUTE_ANNOTATION_NAME, scope)
        }

        return routeAnnotationWrapper
    }

    private fun getMethodWrapper(psiElement: PsiElement?, scope: GlobalSearchScope): PsiMethod? {
        if (null == routeMethodWrapper) {
            val routerClass = JavaPsiFacade.getInstance(psiElement?.project).findClass(ROUTER_FULL_NAME, scope) ?: return null
            routeMethodWrapper = routerClass.findMethodsByName("build",false)[0]
        }

        return routeMethodWrapper
    }
}