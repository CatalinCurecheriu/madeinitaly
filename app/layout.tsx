import type { Metadata } from 'next';
import './globals.css';

export const metadata: Metadata = {
  title: 'Made in Italy Scanner',
  description: 'Verify product origin by barcode using internal data and public product sources.',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
