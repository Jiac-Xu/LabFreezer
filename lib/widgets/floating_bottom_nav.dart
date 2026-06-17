import 'dart:ui' as ui;

import 'package:flutter/material.dart';

/// iOS 风格悬浮底部导航栏
///
/// 使用方式：
/// ```dart
/// Scaffold(
///   extendBody: true,
///   bottomNavigationBar: FloatingBottomNav(
///     currentIndex: _index,
///     items: const [
///       BottomNavItem(label: '首页', icon: Icons.home_rounded),
///       BottomNavItem(label: '标签', icon: Icons.label_rounded),
///     ],
///     onTap: (i) => setState(() => _index = i),
///   ),
///   body: IndexedStack(
///     index: _index,
///     children: pages,
///   ),
/// )
/// ```
class FloatingBottomNav extends StatelessWidget {
  final int currentIndex;
  final List<BottomNavItem> items;
  final ValueChanged<int> onTap;
  final double bottomMargin;

  const FloatingBottomNav({
    super.key,
    required this.currentIndex,
    required this.items,
    required this.onTap,
    this.bottomMargin = 16,
  });

  @override
  Widget build(BuildContext context) {
    final colorScheme = Theme.of(context).colorScheme;

    return SafeArea(
      top: false,
      minimum: const EdgeInsets.only(bottom: 8),
      child: Padding(
        padding: EdgeInsets.fromLTRB(16, 0, 16, bottomMargin),
        child: ClipRRect(
          borderRadius: BorderRadius.circular(24),
          child: BackdropFilter(
            filter: ui.ImageFilter.blur(sigmaX: 20, sigmaY: 20),
            child: Container(
              height: 64,
              decoration: BoxDecoration(
                color: colorScheme.surface.withValues(alpha: 0.82),
                borderRadius: BorderRadius.circular(24),
                boxShadow: [
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.06),
                    blurRadius: 8,
                    offset: const Offset(0, 2),
                  ),
                  BoxShadow(
                    color: Colors.black.withValues(alpha: 0.04),
                    blurRadius: 24,
                    offset: const Offset(0, 8),
                  ),
                ],
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceAround,
                crossAxisAlignment: CrossAxisAlignment.center,
                children: List.generate(items.length, (i) {
                  final item = items[i];
                  final selected = i == currentIndex;
                  return Expanded(
                    child: GestureDetector(
                      behavior: HitTestBehavior.opaque,
                      onTap: () => onTap(i),
                      child: AnimatedContainer(
                        duration: const Duration(milliseconds: 200),
                        margin: const EdgeInsets.symmetric(vertical: 8),
                        decoration: BoxDecoration(
                          color: selected
                              ? colorScheme.primaryContainer
                                  .withValues(alpha: 0.6)
                              : Colors.transparent,
                          borderRadius: BorderRadius.circular(18),
                        ),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            Icon(
                              item.icon,
                              size: 24,
                              color: selected
                                  ? colorScheme.primary
                                  : colorScheme.outline,
                            ),
                            const SizedBox(height: 3),
                            Text(
                              item.label,
                              style: TextStyle(
                                fontSize: 10,
                                fontWeight: selected
                                    ? FontWeight.w600
                                    : FontWeight.normal,
                                color: selected
                                    ? colorScheme.primary
                                    : colorScheme.outline,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  );
                }),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class BottomNavItem {
  final String label;
  final IconData icon;

  const BottomNavItem({required this.label, required this.icon});
}
