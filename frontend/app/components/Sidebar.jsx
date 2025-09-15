"use client";
import React from "react";
import { Home, BarChart2, LogOut, Wallet, Shield } from "lucide-react";
import { useAuth } from "../contexts/AuthContext";
// Minimal Tooltip implementation
function TooltipProvider({ children }) { return children; }
function Tooltip({ children }) { return children; }
function TooltipTrigger({ asChild, children }) { return children; }
function TooltipContent({ children }) { return <span className="ml-2 text-xs text-gray-400">{children}</span>; }

// Minimal Separator implementation
function Separator({ className }) {
	return <hr className={"border-t border-gray-200 " + (className || "")} />;
}

// Minimal Sheet implementation (not used in sidebar, but stubbed)
function Sheet({ children }) { return children; }
function SheetContent({ children }) { return children; }

const navigationItems = [
	{
		label: "Dashboard",
		href: "/dashboard",
		icon: <Home className="w-5 h-5" />,
	},
	{
		label: "Zerodha",
		href: "/zerodha",
		icon: <BarChart2 className="w-5 h-5" />,
	},
	{
		label: "Wallet",
		href: "/wallet",
		icon: <Wallet className="w-5 h-5" />,
	},
	{
		label: "Calculator",
		href: "/calculator",
		icon: <BarChart2 className="w-5 h-5 rotate-90" />,
	},
	{
		label: "Security",
		href: "/security",
		icon: <Shield className="w-5 h-5" />,
	},
];

export function Sidebar({ isCollapsed, setIsCollapsed }) {
	const { user, logout } = useAuth();
	return (
		<aside
			className={`sticky top-0 left-0 h-screen ${isCollapsed ? 'w-20' : 'w-64'} bg-white border-r border-gray-200 flex flex-col shadow-lg z-30 transition-all duration-200`}
			style={{ minHeight: '100vh', maxHeight: '100vh' }}
		>
			<nav className={`flex-1 py-6 px-2 space-y-2 overflow-y-auto transition-all duration-200 ${isCollapsed ? 'px-1' : 'px-4'}`}>
				<TooltipProvider>
					{navigationItems.map((item) => (
						<Tooltip key={item.label} delayDuration={0}>
							<TooltipTrigger asChild>
								<a
									href={item.href}
									className={`flex items-center gap-3 px-3 py-2 rounded-lg text-gray-700 hover:bg-blue-100 hover:text-blue-700 transition-all duration-150 font-medium ${isCollapsed ? 'justify-center' : ''}`}
									style={{ minWidth: 0 }}
								>
									{item.icon}
									{!isCollapsed && <span className="truncate">{item.label}</span>}
								</a>
							</TooltipTrigger>
							<TooltipContent>{item.label}</TooltipContent>
						</Tooltip>
					))}
				</TooltipProvider>
				<Separator className="my-6" />
				<button
					onClick={logout}
					className={`flex items-center gap-3 px-3 py-2 rounded-lg text-red-600 hover:bg-red-100 transition-all duration-150 font-medium ${isCollapsed ? 'justify-center' : ''}`}
				>
					<LogOut className="w-5 h-5" />
					{!isCollapsed && <span>Logout</span>}
				</button>
			</nav>
			<div className={`mt-auto p-4 text-xs text-gray-400 text-center border-t border-gray-100 bg-white transition-all duration-200 ${isCollapsed ? 'hidden' : 'block'}`}>
				<span>© {new Date().getFullYear()} coinTrack</span>
			</div>
		</aside>
	);
}