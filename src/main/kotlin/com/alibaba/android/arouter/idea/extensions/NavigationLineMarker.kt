package com.alibaba.android.arouter.idea.extensions

import com.intellij.codeHighlighting.Pass
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.util.IconLoader
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.java.PsiLiteralExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import com.intellij.psi.impl.source.tree.java.PsiReferenceExpressionImpl
import org.jetbrains.annotations.NotNull
import org.jetbrains.kotlin.asJava.builder.toLightClassOrigin
import java.awt.event.MouseEvent
import java.util.function.Supplier

/**
 * Mark navigation target.
 *
 * @author zhilong <a href="mailto:zhilong.liu@aliyun.com">Contact me.</a>
 * @version 1.0
 * @since 2018/12/13 12:30 PM
 */
class NavigationLineMarker : LineMarkerProvider, GutterIconNavigationHandler<PsiElement> {

    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
        return if (isNavigationCall(element)) {
            LineMarkerInfo(element,element.textRange, navigationOnIcon,null,this,GutterIconRenderer.Alignment.LEFT,
                Supplier { "ARouter Marker" })
        } else {
            null
        }
    }

    override fun navigate(e: MouseEvent?, psiElement: PsiElement?) {
        if (psiElement is PsiMethodCallExpression) {
            val psiExpressionList = (psiElement as PsiMethodCallExpressionImpl).argumentList
            if (psiExpressionList.expressions.size == 1) {
                // Support `build(path)` only now.
                    //(psiExpressionList.expressions[0] as PsiReferenceExpressionImpl).resolve().children
                    //PsiReferenceExpression:testjava
                    //PsiLiteralExpression:"/test/java"
                val targetPath = resolvePath(psiExpressionList.expressions[0])
                val found = NavigationHelper.findTargetAndNavigate(psiElement,targetPath,e)
                if (found){
                    return
                }
            }
        }

        notifyNotFound()
    }



    private fun notifyNotFound() {
        Notifications.Bus.notify(Notification(NOTIFY_SERVICE_NAME, NOTIFY_TITLE, NOTIFY_NO_TARGET_TIPS, NotificationType.WARNING))
    }


    override fun collectSlowLineMarkers(
        elements: @NotNull MutableList<out PsiElement>,
        result: @NotNull MutableCollection<in LineMarkerInfo<*>>
    ) {}

    /**
     * Judge whether the code used for navigation.
     */
    private fun isNavigationCall(psiElement: PsiElement): Boolean {
        if (psiElement is PsiCallExpression) {
            val method = psiElement.resolveMethod() ?: return false
            val parent = method.parent
            if (method.name == "build" && parent is PsiClass) {
                if (isClassOfARouter(parent)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Judge whether the caller was ARouter
     */
    private fun isClassOfARouter(psiClass: PsiClass): Boolean {
        // It was ARouter
        if (psiClass.name.equals(SDK_NAME)) {
            return true
        }

        // It super class was ARouter
        psiClass.supers.find { it.name == SDK_NAME } ?: return false

        return true
    }

    companion object {
        const val SDK_NAME = "ARouter"

        // Notify
        const val NOTIFY_SERVICE_NAME = "ARouter Plugin Tips"
        const val NOTIFY_TITLE = "Road Sign"
        const val NOTIFY_NO_TARGET_TIPS = "No destination found or unsupported type."

        val navigationOnIcon = IconLoader.getIcon("/icon/outline_my_location_black_18dp.png")

        //获取路径
        fun resolvePath(element:Any):String{
            val path = when(element){
                is PsiLiteralExpressionImpl -> element.text
                is PsiReferenceExpressionImpl ->{
                    val children = element.resolve()!!.children
                    var target = children.findLast { it is PsiLiteralExpressionImpl}
                    if (children.isEmpty()){//应该是KT里面的常量引用
                        target = element.resolve()
                        target = target!!.navigationElement.lastChild
                    }
                    target!!.text
                }
                else->element.toString()
            }
            return path.replace("\"", "")
        }
    }

}