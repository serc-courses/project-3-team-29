import { NavLink, useLocation } from 'react-router-dom'
import './BottomNav.css'

const INVESTOR_HIDDEN = ['/orders/new']
const ADVISOR_HIDDEN = ['/advisor/orders/new', '/advisor/orders/review']
const DETAIL_PATTERNS = [/^\/orders\/.+$/, /^\/funds\/.+$/, /^\/advisor\/clients\/.+$/]

function HomeIcon() {
  return (
    <svg className="bottom-nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
      <polyline points="9 22 9 12 15 12 15 22"/>
    </svg>
  )
}
function OrdersIcon() {
  return (
    <svg className="bottom-nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
      <polyline points="14 2 14 8 20 8"/>
      <line x1="8" y1="13" x2="16" y2="13"/>
      <line x1="8" y1="17" x2="12" y2="17"/>
    </svg>
  )
}
function FundsIcon() {
  return (
    <svg className="bottom-nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/>
      <polyline points="16 7 22 7 22 13"/>
    </svg>
  )
}
function AccountIcon() {
  return (
    <svg className="bottom-nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
    </svg>
  )
}
function ClientsIcon() {
  return (
    <svg className="bottom-nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
      <circle cx="9" cy="7" r="4"/>
      <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
      <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    </svg>
  )
}
function ActivityIcon() {
  return (
    <svg className="bottom-nav-icon" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"/>
    </svg>
  )
}

const INVESTOR_TABS = [
  { label: 'Home',    path: '/',        Icon: HomeIcon,    end: true  },
  { label: 'Orders',  path: '/orders',  Icon: OrdersIcon,  end: true  },
  { label: 'Funds',   path: '/funds',   Icon: FundsIcon,   end: true  },
  { label: 'Account', path: '/account', Icon: AccountIcon, end: true  },
]

const ADVISOR_TABS = [
  { label: 'Home',     path: '/advisor',          Icon: HomeIcon,     end: true  },
  { label: 'Clients',  path: '/advisor/clients',  Icon: ClientsIcon,  end: true  },
  { label: 'Activity', path: '/advisor/activity', Icon: ActivityIcon, end: true  },
  { label: 'Account',  path: '/account',          Icon: AccountIcon,  end: true  },
]

export default function BottomNav({ isAdvisor = false }) {
  const location = useLocation()
  const pathname = location.pathname

  const isHidden =
    INVESTOR_HIDDEN.includes(pathname) ||
    ADVISOR_HIDDEN.includes(pathname) ||
    DETAIL_PATTERNS.some(p => p.test(pathname))

  if (isHidden) return null

  const tabs = isAdvisor ? ADVISOR_TABS : INVESTOR_TABS

  return (
    <nav className="bottom-nav">
      {tabs.map(({ label, path, Icon, end }) => (
        <NavLink
          key={path}
          to={path}
          end={end}
          className={({ isActive }) => `bottom-nav-item${isActive ? ' active' : ''}`}
        >
          <Icon />
          <span className="bottom-nav-label">{label}</span>
        </NavLink>
      ))}
    </nav>
  )
}
