import { Component } from 'react'

// Catches render-time errors anywhere in the tree and shows a custom 500 page.
export default class ErrorBoundary extends Component {
  constructor(props) {
    super(props)
    this.state = { hasError: false }
  }

  static getDerivedStateFromError() {
    return { hasError: true }
  }

  componentDidCatch(error, info) {
    // eslint-disable-next-line no-console
    console.error('Unhandled UI error:', error, info)
  }

  render() {
    if (this.state.hasError) {
      return (
        <div className="error-page">
          <h1>500</h1>
          <p>Something went wrong on our end.</p>
          <button
            className="btn-primary"
            style={{ maxWidth: 200, margin: '1rem auto' }}
            onClick={() => {
              this.setState({ hasError: false })
              window.location.assign('/')
            }}
          >
            Back to home
          </button>
        </div>
      )
    }
    return this.props.children
  }
}
