package com.lostfound.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lostfound.app.ChatScreen
import com.lostfound.app.ClaimReviewScreen
import com.lostfound.app.HandoverScreen
import com.lostfound.app.NotificationsScreen
import com.lostfound.app.VerifyApiScreen
import com.lostfound.app.core.session.SessionStore
import com.lostfound.app.data.model.ReportReason
import com.lostfound.app.data.model.ReportTarget
import com.lostfound.app.ui.auth.ChangePasswordScreen
import com.lostfound.app.ui.auth.ForgotPasswordScreen
import com.lostfound.app.ui.auth.LoginScreen
import com.lostfound.app.ui.auth.RegisterScreen
import com.lostfound.app.ui.auth.SplashScreen
import com.lostfound.app.ui.home.HomeScreen
import com.lostfound.app.ui.myposts.MyPostsScreen
import com.lostfound.app.ui.post.CreatePostScreen
import com.lostfound.app.ui.profile.EditProfileScreen
import com.lostfound.app.ui.profile.ProfileScreen
import com.lostfound.app.ui.report.ReportScreen
import com.lostfound.app.ui.report.SentReportsScreen
import com.lostfound.app.ui.search.SearchScreen

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, "Home", Icons.Filled.Home),
    Tab(Routes.MY_POSTS, "Bài của bạn", Icons.AutoMirrored.Filled.List),
    Tab(Routes.CREATE_POST, "Đăng bài", Icons.Filled.Add),
    Tab(Routes.MESSAGES, "Tin nhắn", Icons.Filled.Email),
    Tab(Routes.PROFILE, "Cá nhân", Icons.Filled.Person)
)

/** Mở một tab của thanh điều hướng dưới, giữ trạng thái của tab khi quay lại. */
fun NavHostController.openTab(route: String) {
    navigate(route) {
        // Home luôn là gốc sau khi đăng nhập
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Xóa toàn bộ lịch sử rồi mở [route] (dùng khi đăng nhập / đăng xuất). */
fun NavHostController.resetTo(route: String) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val session by SessionStore.session.collectAsState()

    // Phiên bị xóa (đăng xuất, refresh token hết hạn, tài khoản bị khóa) → về màn Đăng nhập
    LaunchedEffect(session, currentRoute) {
        if (session == null && currentRoute != null && currentRoute !in Routes.AUTH_ROUTES) {
            navController.resetTo(Routes.LOGIN)
        }
    }

    val openReport: (ReportTarget, String, String?) -> Unit = { target, id, title ->
        navController.navigate(Routes.report(target, id, title = title))
    }

    Scaffold(
        // Mỗi màn tự xử lý thanh trạng thái; Scaffold chỉ chừa chỗ cho thanh điều hướng dưới
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (currentRoute in Routes.TAB_ROUTES) {
                BottomBar(currentRoute = currentRoute, onSelect = { route ->
                    if (route == Routes.CREATE_POST) navController.navigate(route)
                    else navController.openTab(route)
                })
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ---------------- Khởi động & tài khoản ----------------
            composable(Routes.SPLASH) {
                SplashScreen(
                    onLoggedIn = { navController.resetTo(Routes.HOME) },
                    onLoggedOut = { navController.resetTo(Routes.LOGIN) }
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoggedIn = { navController.resetTo(Routes.HOME) },
                    onRegister = { navController.navigate(Routes.REGISTER) },
                    onForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    onRegistered = { navController.resetTo(Routes.HOME) },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.FORGOT_PASSWORD) {
                ForgotPasswordScreen(
                    onDone = { navController.resetTo(Routes.HOME) },
                    onBack = { navController.popBackStack() }
                )
            }

            // ---------------- 4 tab chính ----------------
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenPost = { navController.navigate(Routes.detail(it)) },
                    onOpenSearch = { navController.navigate(Routes.SEARCH) },
                    onOpenNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                    onOpenPendingVerifications = { navController.navigate(Routes.claimReview()) },
                    onOpenUnreadMessages = { navController.openTab(Routes.MESSAGES) },
                    onOpenAwaitingHandover = { navController.navigate(Routes.HANDOVER) },
                    onCreatePost = { navController.navigate(Routes.CREATE_POST) },
                    onReport = openReport
                )
            }
            composable(Routes.MY_POSTS) {
                MyPostsScreen(
                    onOpenPost = { navController.navigate(Routes.detail(it)) },
                    onOpenClaims = { navController.navigate(Routes.claimReview(it)) },
                    onCreatePost = { navController.navigate(Routes.CREATE_POST) }
                )
            }
            composable(Routes.MESSAGES) {
                MessagesTabScreen(onOpenChat = { navController.navigate(Routes.CHAT) })
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    onEditProfile = { navController.navigate(Routes.EDIT_PROFILE) },
                    onMyPosts = { navController.openTab(Routes.MY_POSTS) },
                    onSentReports = { navController.navigate(Routes.SENT_REPORTS) },
                    onChangePassword = { navController.navigate(Routes.CHANGE_PASSWORD) },
                    onLoggedOut = { navController.resetTo(Routes.LOGIN) }
                )
            }

            // ---------------- Màn của B ----------------
            composable(Routes.SEARCH) {
                SearchScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { navController.navigate(Routes.detail(it)) },
                    onReport = openReport
                )
            }
            composable(Routes.CREATE_POST) {
                CreatePostScreen(
                    onBack = { navController.popBackStack() },
                    onCreated = { postId ->
                        navController.popBackStack()
                        navController.navigate(Routes.detail(postId))
                    }
                )
            }
            composable(Routes.EDIT_PROFILE) {
                EditProfileScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.CHANGE_PASSWORD) {
                ChangePasswordScreen(onBack = { navController.popBackStack() })
            }
            composable(Routes.SENT_REPORTS) {
                SentReportsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenPost = { navController.navigate(Routes.detail(it)) }
                )
            }
            composable(
                Routes.REPORT,
                arguments = listOf(
                    navArgument("target") { type = NavType.StringType },
                    navArgument("targetId") { type = NavType.StringType },
                    navArgument("reason") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("title") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { entry ->
                val args = entry.arguments
                val target = ReportTarget.fromCode(args?.getString("target")) ?: ReportTarget.POST
                ReportScreen(
                    target = target,
                    targetId = args?.getString("targetId").orEmpty(),
                    initialReason = ReportReason.fromCode(args?.getString("reason")),
                    targetTitle = args?.getString("title"),
                    onBack = { navController.popBackStack() },
                    onOpenSentReports = {
                        navController.popBackStack()
                        navController.navigate(Routes.SENT_REPORTS)
                    }
                )
            }

            // ---------------- Màn của C (gắn vào điều hướng) ----------------
            composable(
                Routes.DETAIL,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { entry ->
                val postId = entry.arguments?.getString("postId").orEmpty()
                PostDetailRoute(
                    postId = postId,
                    onBack = { navController.popBackStack() },
                    onVerify = { navController.navigate(Routes.verify(postId)) },
                    onOpenClaims = { navController.navigate(Routes.claimReview(postId)) },
                    onReport = openReport
                )
            }
            composable(
                Routes.VERIFY,
                arguments = listOf(navArgument("postId") { type = NavType.StringType })
            ) { entry ->
                VerifyApiScreen(
                    postId = entry.arguments?.getString("postId").orEmpty(),
                    onBack = { navController.popBackStack() },
                    onSubmitSuccess = { _, _ -> navController.popBackStack() }
                )
            }
            composable(
                Routes.CLAIM_REVIEW,
                arguments = listOf(
                    navArgument("postId") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                // TODO(C): khi màn Duyệt dùng dữ liệu thật, nhận postId từ entry.arguments
                ClaimReviewScreen(
                    onDecision = { _, decision ->
                        if (decision == "PASS") navController.navigate(Routes.CHAT)
                    }
                )
            }
            composable(Routes.CHAT) {
                ChatScreen(onStartHandover = { navController.navigate(Routes.HANDOVER) })
            }
            composable(Routes.HANDOVER) {
                HandoverScreen(
                    onConfirmReceived = { navController.navigate(Routes.NOTIFICATIONS) },
                    // TODO(C): khi màn Trao trả có mã hội thoại thật, mở form báo cáo bằng:
                    // navController.navigate(Routes.report(ReportTarget.CONVERSATION, conversationId,
                    //     reason = ReportReason.OTHER, title = "Chưa nhận được đồ / có vấn đề"))
                    onReportProblem = { navController.navigate(Routes.SENT_REPORTS) }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationsScreen()
            }
        }
    }
}

@Composable
private fun BottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        TABS.forEach { tab ->
            val isCreate = tab.route == Routes.CREATE_POST
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onSelect(tab.route) },
                icon = {
                    if (isCreate) {
                        // Nút + nổi bật ở giữa
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                tab.icon,
                                contentDescription = tab.label,
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    } else {
                        Icon(tab.icon, contentDescription = tab.label)
                    }
                },
                label = if (isCreate) null else ({ Text(tab.label, maxLines = 1) }),
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
