import React, { useState, useEffect, useMemo } from 'react';
import './App.css';

function App() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [refreshing, setRefreshing] = useState(false);

    const API_URL = 'http://localhost:8080/api';

    const fetchData = async (endpoint) => {
        try {
            const response = await fetch(`${API_URL}${endpoint}`);
            if (!response.ok) throw new Error("Backend server error.");
            const result = await response.json();
            setData(result);
        } catch (err) {
            console.error("Failed to fetch data:", err);
        } finally {
            setLoading(false);
            setRefreshing(false);
        }
    };

    useEffect(() => {
        fetchData('/stats');
    }, []);

    const handleRefresh = async () => {
        setRefreshing(true);
        await fetchData('/refresh');
    };

    if (loading || !data) {
        return <div className="loading">Loading EuroMillions Data...</div>;
    }

    const isButtonDisabled = refreshing || !data.isNewDrawPending;
    let buttonText = refreshing ? "Checking..." : (!data.isNewDrawPending ? "Up to Date" : "Check for New Results");

    return (
        <div className="container">
            <header className="header">
                <div>
                    <h2>EuroMillions Analytics</h2>
                    <p className="subtitle">
                        DB Status: Draw <strong>#{data.latestDbDrawNumber}</strong> ({data.latestDbDate})
                    </p>
                </div>
                <button onClick={handleRefresh} disabled={isButtonDisabled}>
                    {buttonText}
                </button>
            </header>

            <div className="tables-container">
                <StatsTable title="Main Numbers (1-50)" stats={data.mainNumbers || []} isStar={false} />
                <StatsTable title="Stars (1-12)" stats={data.stars || []} isStar={true} />
            </div>
        </div>
    );
}

function StatsTable({ title, stats, isStar }) {
    const [sortConfig, setSortConfig] = useState({ key: 'number', direction: 'asc' });

    const sortedStats = useMemo(() => {
        let sortableItems = [...stats];
        if (sortConfig !== null) {
            sortableItems.sort((a, b) => {
                let aValue = a[sortConfig.key];
                let bValue = b[sortConfig.key];

                if (aValue === 'N/A') aValue = -1;
                if (bValue === 'N/A') bValue = -1;

                if (typeof aValue === 'string' && aValue.endsWith('%')) {
                    aValue = parseFloat(aValue.replace('%', ''));
                    bValue = parseFloat(bValue.replace('%', ''));
                }

                const floatColumns = ['overallAvg', 'avg3Months', 'avg6Months', 'avg9Months', 'avg12Months'];
                if (floatColumns.includes(sortConfig.key)) {
                    aValue = parseFloat(aValue);
                    bValue = parseFloat(bValue);
                }

                if (sortConfig.key === 'lastDrawnDate' || sortConfig.key === 'expectedDrawDate') {
                    aValue = aValue !== -1 ? new Date(aValue).getTime() : -1;
                    bValue = bValue !== -1 ? new Date(bValue).getTime() : -1;
                }

                return sortConfig.direction === 'asc' ? (aValue < bValue ? -1 : 1) : (aValue > bValue ? -1 : 1);
            });
        }
        return sortableItems;
    }, [stats, sortConfig]);

    const requestSort = (key) => {
        let direction = 'asc';
        if (sortConfig.key === key && sortConfig.direction === 'asc') direction = 'desc';
        setSortConfig({ key, direction });
    };

    const getSortIcon = (col) => sortConfig.key !== col ? null : <span className="sort-icon">{sortConfig.direction === 'asc' ? '▲' : '▼'}</span>;
    const isOverdue = (dateString) => dateString !== 'N/A' && new Date(dateString) < new Date();

    return (
        <div className="table-container">
            <h3>{title}</h3>
            <table>
                <thead>
                <tr>
                    <th onClick={() => requestSort('number')}>{isStar ? 'Star' : 'Number'} {getSortIcon('number')}</th>
                    <th onClick={() => requestSort('hits')}>Hits {getSortIcon('hits')}</th>
                    <th onClick={() => requestSort('percentage')}>% {getSortIcon('percentage')}</th>
                    <th onClick={() => requestSort('overallAvg')}>Overall Avg {getSortIcon('overallAvg')}</th>
                    <th onClick={() => requestSort('avg12Months')}>12M Avg {getSortIcon('avg12Months')}</th>
                    <th onClick={() => requestSort('avg9Months')}>9M Avg {getSortIcon('avg9Months')}</th>
                    <th onClick={() => requestSort('avg6Months')}>6M Avg {getSortIcon('avg6Months')}</th>
                    <th onClick={() => requestSort('avg3Months')}>3M Avg {getSortIcon('avg3Months')}</th>
                    <th onClick={() => requestSort('lastDrawnDate')}>Last Drawn {getSortIcon('lastDrawnDate')}</th>
                    <th onClick={() => requestSort('expectedDrawDate')}>Expected Date {getSortIcon('expectedDrawDate')}</th>
                </tr>
                </thead>
                <tbody>
                {sortedStats.map((stat) => (
                    <tr key={stat.number}>
                        <td className="font-bold">{stat.number}</td>
                        <td>{stat.hits}</td>
                        <td className="highlight-percent">{stat.percentage}</td>
                        <td>{stat.overallAvg}</td>
                        <td>{stat.avg12Months}</td>
                        <td>{stat.avg9Months}</td>
                        <td>{stat.avg6Months}</td>
                        <td>{stat.avg3Months}</td>
                        <td>{stat.lastDrawnDate}</td>
                        <td style={{ color: isOverdue(stat.expectedDrawDate) ? '#d32f2f' : 'inherit', fontWeight: isOverdue(stat.expectedDrawDate) ? 'bold' : 'normal' }}>
                            {stat.expectedDrawDate}
                        </td>
                    </tr>
                ))}
                </tbody>
            </table>
        </div>
    );
}

export default App;