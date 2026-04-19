import { PieChart, Pie, Cell, Tooltip, ResponsiveContainer } from 'recharts'

const TOOLTIP_STYLE = {
  background: '#ffffff',
  border: '1px solid #E2E8F0',
  borderRadius: 8,
  boxShadow: '0 4px 12px rgba(0,0,0,0.08)',
  fontSize: 13,
  fontFamily: 'Inter, sans-serif',
  padding: '6px 10px',
}

export default function StatusDonut({
  data,             // [{ name, value, color }]
  innerRadius = 50,
  outerRadius = 80,
  height = 180,
}) {
  const total = data.reduce((s, d) => s + d.value, 0)

  if (total === 0) {
    return (
      <div style={{
        height,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'var(--color-text-400)',
        fontSize: 13,
        fontStyle: 'italic',
      }}>
        No data
      </div>
    )
  }

  return (
    <div>
      <div style={{ position: 'relative', height }}>
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              innerRadius={innerRadius}
              outerRadius={outerRadius}
              dataKey="value"
              strokeWidth={0}
              isAnimationActive
              animationDuration={600}
              animationEasing="ease-out"
            >
              {data.map(d => <Cell key={d.name} fill={d.color} />)}
            </Pie>
            <Tooltip
              contentStyle={TOOLTIP_STYLE}
              formatter={(v, n) => [`${v} orders`, n]}
              separator=""
            />
          </PieChart>
        </ResponsiveContainer>
        {/* Center label overlaid via absolute div */}
        <div style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          textAlign: 'center',
          pointerEvents: 'none',
          lineHeight: 1,
        }}>
          <div style={{ fontSize: 22, fontWeight: 700, color: 'var(--color-text-900)' }}>{total}</div>
          <div style={{ fontSize: 10, color: 'var(--color-text-400)', marginTop: 3, textTransform: 'uppercase', letterSpacing: '0.05em' }}>orders</div>
        </div>
      </div>
      {/* Legend */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px 14px', justifyContent: 'center', marginTop: 8 }}>
        {data.filter(d => d.value > 0).map(d => (
          <span key={d.name} style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 12, color: 'var(--color-text-500)' }}>
            <span style={{ width: 8, height: 8, borderRadius: '50%', background: d.color, flexShrink: 0 }} />
            {d.name} ({d.value})
          </span>
        ))}
      </div>
    </div>
  )
}
