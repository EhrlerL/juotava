import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';
//import 'bootstrap/dist/css/bootstrap.min.css';
import 'material-icons/iconfont/material-icons.css';
import './css/test.css';
import { Auth0Provider } from '@auth0/auth0-react';
import { audience, baseUrlAuth, clientId } from './config/config';

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(
    <Auth0Provider
      domain={baseUrlAuth}
      clientId={clientId}
      authorizationParams={{
        redirect_uri: window.location.origin,
        audience: audience
      }}>
      <App />
    </Auth0Provider>
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();
