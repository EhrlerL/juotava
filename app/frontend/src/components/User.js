import React from 'react';
import { useNavigate } from 'react-router';

function User(props) {
    let style = { width: 'auto', cursor: 'pointer', padding: '0' };
    if(props.disabled) {
        style = { width: 'auto', cursor: 'not-allowed', padding: '0', opacity: '0.5' };
    }
    const navigate = useNavigate();
    const cyClass = (props.cyClass !== undefined) ? " "+props.cyClass : "";
    return (
        <h1>User page</h1>
    );
}
export default User;