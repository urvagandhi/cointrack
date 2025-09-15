// Stock and fund name mapping utility
const stockNames = {
    // Stock symbols to company names
    'HDBFS': 'HDFC Financial Services Ltd',
    'TTML': 'Tata Teleservices (Maharashtra)',

    // Add more stock mappings as needed
    'RELIANCE': 'Reliance Industries Ltd',
    'TCS': 'Tata Consultancy Services',
    'INFY': 'Infosys Ltd',
    'HDFC': 'HDFC Bank Ltd',
    'ICICIBANK': 'ICICI Bank Ltd',
    'KOTAKBANK': 'Kotak Mahindra Bank',
    'SBIN': 'State Bank of India',
    'BHARTIARTL': 'Bharti Airtel Ltd',
    'ITC': 'ITC Ltd',
    'HCLTECH': 'HCL Technologies',
    'WIPRO': 'Wipro Ltd',
    'ASIANPAINT': 'Asian Paints Ltd',
    'MARUTI': 'Maruti Suzuki India Ltd',
    'LT': 'Larsen & Toubro Ltd',
    'ULTRACEMCO': 'UltraTech Cement Ltd',
    'NESTLEIND': 'Nestle India Ltd',
    'TITAN': 'Titan Company Ltd',
    'BAJFINANCE': 'Bajaj Finance Ltd',
    'AXISBANK': 'Axis Bank Ltd',
    'POWERGRID': 'Power Grid Corporation',
    'TECHM': 'Tech Mahindra Ltd',
    'SUNPHARMA': 'Sun Pharmaceutical Industries',
    'TATASTEEL': 'Tata Steel Ltd',
    'ONGC': 'Oil & Natural Gas Corporation',
    'NTPC': 'NTPC Ltd',
    'COALINDIA': 'Coal India Ltd',
    'BAJAJFINSV': 'Bajaj Finserv Ltd',
    'HDFCLIFE': 'HDFC Life Insurance',
    'BRITANNIA': 'Britannia Industries Ltd',
    'DRREDDY': 'Dr. Reddy\'s Laboratories',
    'EICHERMOT': 'Eicher Motors Ltd',
    'GRASIM': 'Grasim Industries Ltd',
    'HINDALCO': 'Hindalco Industries Ltd',
    'INDUSINDBK': 'IndusInd Bank Ltd',
    'JSWSTEEL': 'JSW Steel Ltd',
    'M&M': 'Mahindra & Mahindra Ltd',
    'CIPLA': 'Cipla Ltd',
    'DIVISLAB': 'Divi\'s Laboratories Ltd',
    'TATACONSUM': 'Tata Consumer Products Ltd',
    'ADANIPORTS': 'Adani Ports and SEZ Ltd',
};

const fundNames = {
    // Mutual fund ISIN to short names
    'INF174KA1HV3': 'Kotak Multicap Fund - Direct',
    'INF966L01AT0': 'Quant Large Cap Fund - Direct',
    'INF247L01445': 'Motilal Oswal Midcap Fund - Direct',
    'INF247L01BQ9': 'Motilal Oswal Nifty Microcap 250 Index Fund - Direct',

    // Add more fund mappings as needed
    'INF247L01GQ7': 'SBI Large Cap Fund - Direct',
    'INF247L01BQ9': 'SBI Small Cap Fund - Direct',
    'INF090I01239': 'ICICI Prudential Bluechip Fund - Direct',
    'INF966L01AT0': 'UTI Nifty 50 Index Fund - Direct',
};

export function getShortCompanyName(symbol) {
    if (!symbol) return 'Unknown';

    // Check if it's a fund ISIN first
    if (fundNames[symbol]) {
        return fundNames[symbol];
    }

    // Check if it's a stock symbol
    if (stockNames[symbol]) {
        return stockNames[symbol];
    }

    // Return the symbol itself if no mapping found
    return symbol;
}

export function getFundName(fundName, tradingSymbol) {
    // If we have a proper fund name from API, use it
    if (fundName && fundName !== tradingSymbol) {
        return fundName;
    }

    // Otherwise, try to map from ISIN
    return getShortCompanyName(tradingSymbol);
}

export function formatFundName(fullFundName) {
    if (!fullFundName) return 'Unknown Fund';

    // Clean up fund names for better display
    return fullFundName
        .replace(/\s*-\s*DIRECT PLAN$/i, ' - Direct')
        .replace(/\s+/g, ' ')
        .trim();
}