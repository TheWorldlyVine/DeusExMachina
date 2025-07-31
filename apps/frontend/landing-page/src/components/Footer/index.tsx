import styles from './Footer.module.css'

export function Footer() {
  return (
    <footer className={styles.footer}>
      <div className="container">
        <p>&copy; 2024 WorldBuilder. All rights reserved.</p>
      </div>
    </footer>
  )
}