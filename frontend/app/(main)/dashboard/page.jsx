'use client';

import { useAuth } from '../../contexts/AuthContext';
import Link from 'next/link';

export default function Dashboard() {
    const { user } = useAuth();

    return (
        <div className="p-6">
            {/* Header */}
            <div className="mb-8">
                <h1 className="text-3xl font-bold text-gray-900 mb-2">
                    Welcome back, {user?.name || 'User'}!
                </h1>
                <p className="text-gray-600">
                    Track your investments and manage your portfolio from your dashboard.
                </p>
            </div>

            {/* Quick Stats */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
                <div className="bg-white rounded-lg shadow p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <div className="w-8 h-8 bg-green-100 rounded-md flex items-center justify-center">
                                <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
                                </svg>
                            </div>
                        </div>
                        <div className="ml-4">
                            <p className="text-sm font-medium text-gray-500">Total Portfolio</p>
                            <p className="text-2xl font-semibold text-gray-900">₹39,20,150</p>
                        </div>
                    </div>
                </div>

                <div className="bg-white rounded-lg shadow p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <div className="w-8 h-8 bg-blue-100 rounded-md flex items-center justify-center">
                                <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                        <div className="ml-4">
                            <p className="text-sm font-medium text-gray-500">Today's P&L</p>
                            <p className="text-2xl font-semibold text-green-600">+₹2,78,093</p>
                        </div>
                    </div>
                </div>

                <div className="bg-white rounded-lg shadow p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <div className="w-8 h-8 bg-purple-100 rounded-md flex items-center justify-center">
                                <svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M17 9V7a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2m2 4h10a2 2 0 002-2v-6a2 2 0 00-2-2H9a2 2 0 00-2 2v6a2 2 0 002 2zm7-5a2 2 0 11-4 0 2 2 0 014 0z" />
                                </svg>
                            </div>
                        </div>
                        <div className="ml-4">
                            <p className="text-sm font-medium text-gray-500">Active Holdings</p>
                            <p className="text-2xl font-semibold text-gray-900">6</p>
                        </div>
                    </div>
                </div>

                <div className="bg-white rounded-lg shadow p-6">
                    <div className="flex items-center">
                        <div className="flex-shrink-0">
                            <div className="w-8 h-8 bg-orange-100 rounded-md flex items-center justify-center">
                                <svg className="w-5 h-5 text-orange-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
                                </svg>
                            </div>
                        </div>
                        <div className="ml-4">
                            <p className="text-sm font-medium text-gray-500">Active SIPs</p>
                            <p className="text-2xl font-semibold text-gray-900">2</p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Quick Actions */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
                <div className="bg-white rounded-lg shadow p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">Quick Actions</h3>
                    <div className="space-y-3">
                        <Link
                            href="/zerodha/dashboard"
                            className="flex items-center p-3 bg-blue-50 rounded-lg hover:bg-blue-100 transition-colors"
                        >
                            <div className="w-8 h-8 bg-blue-100 rounded-md flex items-center justify-center mr-3">
                                <svg className="w-5 h-5 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                                </svg>
                            </div>
                            <div>
                                <p className="font-medium text-gray-900">View Zerodha Holdings</p>
                                <p className="text-sm text-gray-500">Check your complete portfolio</p>
                            </div>
                        </Link>

                        <Link
                            href="/profile"
                            className="flex items-center p-3 bg-green-50 rounded-lg hover:bg-green-100 transition-colors"
                        >
                            <div className="w-8 h-8 bg-green-100 rounded-md flex items-center justify-center mr-3">
                                <svg className="w-5 h-5 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" />
                                </svg>
                            </div>
                            <div>
                                <p className="font-medium text-gray-900">Update Profile</p>
                                <p className="text-sm text-gray-500">Manage your account settings</p>
                            </div>
                        </Link>

                        <Link
                            href="/zerodha"
                            className="flex items-center p-3 bg-purple-50 rounded-lg hover:bg-purple-100 transition-colors"
                        >
                            <div className="w-8 h-8 bg-purple-100 rounded-md flex items-center justify-center mr-3">
                                <svg className="w-5 h-5 text-purple-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13.828 10.172a4 4 0 00-5.656 0l-4 4a4 4 0 105.656 5.656l1.102-1.101m-.758-4.899a4 4 0 005.656 0l4-4a4 4 0 00-5.656-5.656l-1.1 1.1" />
                                </svg>
                            </div>
                            <div>
                                <p className="font-medium text-gray-900">Connect Zerodha</p>
                                <p className="text-sm text-gray-500">Link your broker account</p>
                            </div>
                        </Link>
                    </div>
                </div>

                <div className="bg-white rounded-lg shadow p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">Recent Activity</h3>
                    <div className="space-y-4">
                        <div className="flex items-center">
                            <div className="w-2 h-2 bg-green-400 rounded-full mr-3"></div>
                            <div className="flex-1">
                                <p className="text-sm font-medium text-gray-900">SIP executed successfully</p>
                                <p className="text-xs text-gray-500">Motilal Oswal Midcap Fund - ₹5,000</p>
                            </div>
                            <span className="text-xs text-gray-400">2 hours ago</span>
                        </div>

                        <div className="flex items-center">
                            <div className="w-2 h-2 bg-blue-400 rounded-full mr-3"></div>
                            <div className="flex-1">
                                <p className="text-sm font-medium text-gray-900">Portfolio updated</p>
                                <p className="text-xs text-gray-500">Holdings synced from Zerodha</p>
                            </div>
                            <span className="text-xs text-gray-400">1 day ago</span>
                        </div>

                        <div className="flex items-center">
                            <div className="w-2 h-2 bg-purple-400 rounded-full mr-3"></div>
                            <div className="flex-1">
                                <p className="text-sm font-medium text-gray-900">Profile updated</p>
                                <p className="text-xs text-gray-500">Contact information changed</p>
                            </div>
                            <span className="text-xs text-gray-400">3 days ago</span>
                        </div>

                        <div className="flex items-center">
                            <div className="w-2 h-2 bg-orange-400 rounded-full mr-3"></div>
                            <div className="flex-1">
                                <p className="text-sm font-medium text-gray-900">New user registered</p>
                                <p className="text-xs text-gray-500">Welcome to coinTrack!</p>
                            </div>
                            <span className="text-xs text-gray-400">5 days ago</span>
                        </div>
                    </div>
                </div>
            </div>

            {/* Portfolio Overview Chart */}
            <div className="mt-8">
                <div className="bg-white rounded-lg shadow p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">Portfolio Performance</h3>
                    <div className="flex items-center justify-center h-64 bg-gray-50 rounded-lg">
                        <div className="text-center">
                            <svg className="w-16 h-16 text-gray-400 mx-auto mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z" />
                            </svg>
                            <p className="text-gray-500">Portfolio performance chart will be displayed here</p>
                            <p className="text-sm text-gray-400 mt-2">Connect your Zerodha account to view detailed analytics</p>
                        </div>
                    </div>
                </div>
            </div>

            {/* Market Summary */}
            <div className="mt-8">
                <div className="bg-white rounded-lg shadow p-6">
                    <h3 className="text-lg font-semibold text-gray-900 mb-4">Market Summary</h3>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm font-medium text-gray-500">NIFTY 50</p>
                            <p className="text-xl font-semibold text-gray-900">19,674.25</p>
                            <p className="text-sm text-green-600">+124.35 (+0.63%)</p>
                        </div>
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm font-medium text-gray-500">SENSEX</p>
                            <p className="text-xl font-semibold text-gray-900">66,230.15</p>
                            <p className="text-sm text-green-600">+387.20 (+0.59%)</p>
                        </div>
                        <div className="text-center p-4 bg-gray-50 rounded-lg">
                            <p className="text-sm font-medium text-gray-500">BANK NIFTY</p>
                            <p className="text-xl font-semibold text-gray-900">44,856.30</p>
                            <p className="text-sm text-red-600">-245.10 (-0.54%)</p>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}
