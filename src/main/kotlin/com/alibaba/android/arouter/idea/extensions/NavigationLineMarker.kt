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
import com.intellij.psi.impl.source.tree.java.PsiMethodCallExpressionImpl
import java.awt.event.MouseEvent

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
            LineMarkerInfo<PsiElement>(element, element.textRange, navigationOnIcon,
                    Pass.UPDATE_ALL, null, this,
                    GutterIconRenderer.Alignment.LEFT)
        } else {
            null
        }
    }

    override fun navigate(e: MouseEvent?, psiElement: PsiElement?) {
        if (psiElement is PsiMethodCallExpression) {
            val psiExpressionList = (psiElement as PsiMethodCallExpressionImpl).argumentList
            if (psiExpressionList.expressions.size == 1) {
                // Support `build(path)` only now.
                val targetPath = psiExpressionList.expressions[0].text.replace("\"", "")
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


    override fun collectSlowLineMarkers(elements: MutableList<PsiElement>, result: MutableCollection<LineMarkerInfo<PsiElement>>) {}

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
    }

}