import {
  BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
  PieChart, Pie, Cell, Legend,
} from 'recharts'
import './StatusChart.css'
import { ORDER_STATUS_COLORS, CHART_COLORS } from '../../constants/statusColors'

export default function StatusChart({ data = {}, title, type = 'bar' }) {
  const chartData = Object.entries(data).map(([name, value], i) => ({
    name,
    value,
    fill: ORDER_STATUS_COLORS[name]?.text || CHART_COLORS[i % CHART_COLORS.length],
  }))

  return (
    <div className="status-chart">
      {title && <div className="status-chart-title">{title}</div>}
      <ResponsiveContainer width="100%" height={300}>
        {type === 'donut' ? (
          <PieChart>
            <Pie
              data={chartData}
              cx="50%"
              cy="50%"
              innerRadius={60}
              outerRadius={100}
              dataKey="value"
              nameKey="name"
            >
              {chartData.map((entry, i) => (
                <Cell key={entry.name} fill={entry.fill} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        ) : (
          <BarChart data={chartData} layout="vertical" margin={{ left: 60 }}>
            <XAxis type="number" tick={{ fontSize: 12 }} />
            <YAxis type="category" dataKey="name" tick={{ fontSize: 11 }} width={80} />
            <Tooltip />
            <Bar dataKey="value" radius={[0, 4, 4, 0]}>
              {chartData.map((entry, i) => (
                <Cell key={entry.name} fill={entry.fill} />
              ))}
            </Bar>
          </BarChart>
        )}
      </ResponsiveContainer>
    </div>
  )
}
